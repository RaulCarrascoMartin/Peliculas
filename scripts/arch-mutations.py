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
MUT=[
 ('application usa Spring','applicationDoesNotUseSpringNorPersistenceFrameworks',
   lambda: inject('cart/internal/application/usecase/AddToCart.java','public class AddToCart {','private org.springframework.stereotype.Service leak;')),
 ('domain depende de persistence','domainDependsOnNoOtherLayer',
   lambda: inject('cart/internal/domain/service/CartServiceImpl.java','public class CartServiceImpl implements CartService {','private shop.cart.internal.persistence.repository.JdbcCartRepository leak;')),
 ('application depende de persistence','applicationDoesNotDependOnPersistenceInfrastructureNorHttp',
   lambda: inject('cart/internal/application/usecase/AddToCart.java','public class AddToCart {','private shop.cart.internal.persistence.repository.JdbcCartRepository leak;')),
 ('persistence depende de application','persistenceDoesNotDependOnApplicationNorHttp',
   lambda: inject('cart/internal/persistence/repository/JdbcCartRepository.java','public class JdbcCartRepository implements CartRepository {','private shop.cart.internal.application.usecase.AddToCart leak;')),
 ('*View de application que no es record','useCaseDtosAreRecords',
   lambda: newfile('cart/internal/application/dto/BadView.java','package shop.cart.internal.application.dto;\n\npublic class BadView {\n}\n')),
 ('clase de facade que no es record ni excepcion','useCaseDtosAreRecords',
   lambda: newfile('catalog/api/facade/dto/BadDto.java','package shop.catalog.api.facade.dto;\n\npublic class BadDto {\n}\n')),
 ('clase sin @Configuration en app','appHoldsOnlyConfiguration',
   lambda: newfile('app/Rogue.java','package shop.app;\n\npublic class Rogue {\n}\n')),
 ('ciclo catalog -> cart','noCyclesBetweenModules',
   lambda: inject('catalog/internal/domain/service/MovieServiceImpl.java','public class MovieServiceImpl implements MovieService {','private shop.cart.api.facade.CartFacade leak;')),
 ('shared depende de un modulo','sharedAndAppDoNotBeDependedOnByModules',
   lambda: inject('shared/error/GlobalExceptionHandler.java','public class GlobalExceptionHandler {','private shop.catalog.api.facade.CatalogFacade leak;')),
 ('un modulo depende de app','sharedAndAppDoNotBeDependedOnByModules',
   lambda: (newfile('app/PublicConfig.java','package shop.app;\n\n@org.springframework.context.annotation.Configuration\npublic class PublicConfig {\n}\n'),
            inject('cart/internal/domain/service/CartServiceImpl.java','public class CartServiceImpl implements CartService {','private shop.app.PublicConfig leak;'))),
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
