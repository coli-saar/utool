
# Anwendung: python mkrs.py

# TODO: no_q, most_q

from classes import *

R = [
      (E2, [0, 1], D2, [0]),    # ex - def/0

      (D2, [1], D2, [0]),       # def/1 - def/0

      (E2, [0, 1], A2, [1]),    # ex - all/1
      (D2, [1], A2, [1]),       # def/1 - all/1

      (A2, [1], N1, [0]),       # all/1 - neg

      (N1, [0], E2, [0, 1]),    # neg - ex
      (N1, [0], D2, [1]),       # neg - def/1

      (E2, [0, 1], M1, [0]),    # ex - modal
      (D2, [1], M1, [0]),       # def/1 - modal

      (D2, [1], W2, [0]),
      (E2, [0, 1], W2, [0]),
    ]

print '<?xml version="1.0" ?>'
print '<rewrite-system>'

print '<annotator initial="+">'

for q in E2 + C2 + W2 + O2:
    print '<rule annotation="0" label="%s"><hole annotation="0"/><hole annotation="0"/></rule>' % q
    print '<rule annotation="+" label="%s"><hole annotation="+"/><hole annotation="+"/></rule>' % q
    print '<rule annotation="-" label="%s"><hole annotation="-"/><hole annotation="-"/></rule>' % q

for q in D2:
    print '<rule annotation="0" label="%s"><hole annotation="0"/><hole annotation="0"/></rule>' % q
    print '<rule annotation="+" label="%s"><hole annotation="0"/><hole annotation="+"/></rule>' % q
    print '<rule annotation="-" label="%s"><hole annotation="0"/><hole annotation="-"/></rule>' % q

for q in A2:
    print '<rule annotation="+" label="%s"><hole annotation="-"/><hole annotation="+"/></rule>' % q
    print '<rule annotation="-" label="%s"><hole annotation="+"/><hole annotation="-"/></rule>' % q
    print '<rule annotation="0" label="%s"><hole annotation="0"/><hole annotation="0"/></rule>' % q

# _most_q
print '<rule annotation="0" label="%s"><hole annotation="0"/><hole annotation="0"/></rule>' % '_most_q'
print '<rule annotation="+" label="%s"><hole annotation="+"/><hole annotation="+"/></rule>' % '_most_q'
print '<rule annotation="-" label="%s"><hole annotation="-"/><hole annotation="-"/></rule>' % '_most_q'

# _no_q
print '<rule annotation="+" label="%s"><hole annotation="-"/><hole annotation="-"/></rule>' % "_no_q"
print '<rule annotation="-" label="%s"><hole annotation="+"/><hole annotation="+"/></rule>' % "_no_q"
print '<rule annotation="0" label="%s"><hole annotation="0"/><hole annotation="0"/></rule>' % "_no_q"

for l in M1 + O1 + W1:
    print '<rule annotation="+" label="%s"><hole annotation="+"/></rule>' % l
    print '<rule annotation="-" label="%s"><hole annotation="-"/></rule>' % l
    print '<rule annotation="0" label="%s"><hole annotation="0"/></rule>' % l

for l in N1:
    print '<rule annotation="+" label="%s"><hole annotation="-"/></rule>' % l
    print '<rule annotation="-" label="%s"><hole annotation="+"/></rule>' % l
    print '<rule annotation="0" label="%s"><hole annotation="0"/></rule>' % l

print '</annotator>'

print '<rewriting>'

for (q1, h1, q2, h2) in R:
    for _q1 in q1:
        for _h1 in h1:
            for _q2 in q2:
                for _h2 in h2:
                    print '<rule llabel="%s" lhole="%d" rlabel="%s" rhole="%d" annotation="+"/>' % (_q1, _h1, _q2, _h2)
                    print '<rule llabel="%s" lhole="%d" rlabel="%s" rhole="%d" annotation="-"/>' % (_q2, _h2, _q1, _h1)

print '</rewriting>'

print '</rewrite-system>'
