#!/usr/bin/env python3
"""Mutation check of the ArchUnit rules (ArchitectureTest).

For each mutation it injects one architecture violation into src/, runs ArchitectureTest and checks that the rule
meant to catch it fails. A rule that does not fail would be vacuous. After every mutation the source tree is
restored with `git checkout -- src && git clean -fdq src`, so the script refuses to run if src/ has uncommitted
changes (they would be lost).

Usage (from any directory):  MAVEN=/path/to/mvn scripts/arch-mutations.py     (MAVEN defaults to `mvn`)
Exit code: 0 if every rule caught its mutation, 1 otherwise.
"""
import subprocess,re,os,sys
os.chdir(subprocess.run(['git','rev-parse','--show-toplevel'],capture_output=True,text=True,check=True).stdout.strip())
S='src/main/java/shop/'
M=os.environ.get('MAVEN','mvn')
def sh(c): return subprocess.run(c,shell=True,capture_output=True,text=True).stdout
def revert(): sh('git checkout -- src && git clean -fdq src')
def inject(path,anchor,text):
    s=open(S+path).read(); assert anchor in s,path
    open(S+path,'w').write(s.replace(anchor,anchor+'\n    '+text,1))
def newfile(path,content):
    os.makedirs(os.path.dirname(S+path),exist_ok=True); open(S+path,'w').write(content)
SVC='cart/internal/domain/service/CartServiceImpl.java'
SVC_AT='public class CartServiceImpl implements CartService {'
REPO='cart/internal/persistence/repository/CartRepositoryImpl.java'
REPO_AT='public class CartRepositoryImpl implements CartRepository {'
CTRL='cart/api/http/controller/CartController.java'
CTRL_AT='public class CartController implements CartHttpApi {'
MUT=[
 ('domain depende de persistence','domainDependsOnNoOtherLayer',
   lambda: inject(SVC,SVC_AT,'private shop.cart.internal.persistence.repository.CartRepositoryImpl leak;')),
 ('domain conoce HTTP','domainKnowsNeitherHttpNorSqlNorFormatValidation',
   lambda: inject(SVC,SVC_AT,'private org.springframework.http.HttpStatus leak;')),
 ('controller se salta el dominio y usa persistence','controllerDependsOnlyOnTheDomainNeverSkippingLayers',
   lambda: inject(CTRL,CTRL_AT,'private shop.cart.internal.persistence.repository.CartRepositoryImpl leak;')),
 ('controller usa la interfaz del repositorio','controllerDependsOnlyOnTheDomainNeverSkippingLayers',
   lambda: inject(CTRL,CTRL_AT,'private shop.cart.internal.domain.repository.CartRepository leak;')),
 ('persistence depende del controller','persistenceDependsOnTheDomainAndNeverOnTheController',
   lambda: inject(REPO,REPO_AT,'private shop.cart.api.http.controller.CartController leak;')),
 ('JDBC fuera de persistence','jdbcIsUsedOnlyInPersistence',
   lambda: inject(SVC,SVC_AT,'private org.springframework.jdbc.core.simple.JdbcClient leak;')),
 ('el modelo sale del servicio','theModelNeverLeavesTheService',
   lambda: inject(CTRL,CTRL_AT,'private shop.cart.internal.domain.model.Cart leak;')),
 ('clase en application que no es la facade','applicationHoldsOnlyTheFacadeImplementation',
   lambda: newfile('cart/internal/application/usecase/AddToCart.java','package shop.cart.internal.application.usecase;\n\npublic class AddToCart {\n}\n')),
 ('servicio fuera de domain.service','servicesLiveInDomainService',
   lambda: newfile('cart/api/http/controller/RogueService.java','package shop.cart.api.http.controller;\n\npublic interface RogueService {\n}\n')),
 ('implementacion de repositorio fuera de persistence','repositoriesAreDeclaredInTheDomainAndImplementedInPersistence',
   lambda: newfile('cart/internal/domain/repository/RogueRepositoryImpl.java','package shop.cart.internal.domain.repository;\n\npublic class RogueRepositoryImpl {\n}\n')),
 ('mapper fuera de un paquete mapper','mappersLiveInMapperPackages',
   lambda: newfile('cart/internal/domain/service/RogueMapper.java','package shop.cart.internal.domain.service;\n\npublic interface RogueMapper {\n}\n')),
 ('DTO que no es record','dtosLiveInDtoPackagesAndAreRecords',
   lambda: newfile('cart/internal/domain/dto/BadDto.java','package shop.cart.internal.domain.dto;\n\npublic class BadDto {\n}\n')),
 ('clase de facade dto que no es record','dtosLiveInDtoPackagesAndAreRecords',
   lambda: newfile('catalog/api/facade/dto/BadDto.java','package shop.catalog.api.facade.dto;\n\npublic class BadDto {\n}\n')),
 ('excepcion fuera de un paquete exception','exceptionsLiveInExceptionPackagesOrInTheSharedErrors',
   lambda: newfile('cart/internal/domain/service/RogueException.java','package shop.cart.internal.domain.service;\n\npublic class RogueException extends RuntimeException {\n}\n')),
 ('controller fuera de api.http.controller','controllersLiveInHttpController',
   lambda: newfile('cart/internal/domain/service/RogueController.java','package shop.cart.internal.domain.service;\n\npublic class RogueController {\n}\n')),
 ('inyeccion en campo','dependenciesAreInjectedByConstructor',
   lambda: inject(SVC,SVC_AT,'@org.springframework.beans.factory.annotation.Autowired private java.time.Clock leak;')),
 ('un modulo usa el internal de otro','modulesAreAccessedOnlyThroughTheirFacade',
   lambda: inject(SVC,SVC_AT,'private shop.catalog.internal.domain.service.MovieService leak;')),
 ('la facade depende de su internal','facadeDoesNotDependOnItsInternals',
   lambda: newfile('catalog/api/facade/dto/LeakDto.java','package shop.catalog.api.facade.dto;\n\npublic record LeakDto(shop.catalog.internal.domain.service.MovieService leak) {\n}\n')),
 ('ciclo catalog -> cart','noCyclesBetweenModules',
   lambda: inject('catalog/internal/domain/service/MovieServiceImpl.java','public class MovieServiceImpl implements MovieService {','private shop.cart.api.facade.CartFacade leak;')),
 ('shared depende de un modulo','sharedDependsOnNoModule',
   lambda: inject('shared/error/GlobalExceptionHandler.java','public class GlobalExceptionHandler {','private shop.catalog.api.facade.CatalogFacade leak;')),
]
if sh('git status --porcelain -- src').strip():
    sys.exit('src/ has uncommitted changes: commit or stash them first (the script restores src/ after each mutation).')
failures=0
for name,expected,f in MUT:
    revert(); f()
    out=sh(f'{M} -o test -Dtest=ArchitectureTest 2>&1')
    if 'COMPILATION ERROR' in out:
        print(f'??   {name}: does not compile'); failures+=1; continue
    failed=sorted(set(re.findall(r'ArchitectureTest\.(\w+)',' '.join(re.findall(r'\[ERROR\]   ArchitectureTest\.\w+',out)))))
    ok=expected in failed
    failures+=not ok
    print(('OK   ' if ok else 'FAIL ')+f'{name} -> expects {expected}; failing: {failed}')
revert()
print('src clean:',sh('git status --porcelain -- src').strip()=='')
sys.exit(1 if failures else 0)
