
from classes import *

def main():
    print '<equivalences style="ERG">'

    # 1-place wildcards: message types, ...
    for l in W1:
        print '<permutesWithEverything label="%s" hole="0"/>' % l

    # 2-place wildcards: proper names, pronouns, numbers 
    for l in W2:
        print '<permutesWithEverything label="%s" hole="1"/>' % l

    for c in C2:

        print '<equivalencegroup>'

        # Existential quantifiers
        for l in E2:
            print '<quantifier label="%s" hole="0"/>' % l
            print '<quantifier label="%s" hole="1"/>' % l

        # Definites
        for l in D2:
            print '<quantifier label="%s" hole="1"/>' % l

        # Connectives
        print '<quantifier label="%s" hole="0"/>' % c
        print '<quantifier label="%s" hole="1"/>' % c

        print '</equivalencegroup>'

    print '</equivalences>'

if __name__ == '__main__':
    main()
