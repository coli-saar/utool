
## Makefile for LaTeX projects
## Alexander Koller 20.9.2000
## based on Makefile by Joachim Niehren


## Parameters
START = 0
PROJECT = utool-doc
AUTHORS = K
PREAMBLE = preamble.tex


## Components: texts, macros, pictures, bibtex databases
TEXTS = intro.tex tutorial.tex operations.tex codecs.tex \
	integration.tex further.tex conclusion.tex building.tex \
	advanced.tex practice.tex

MACROS =  tree-macros.sty local-macros.tex

PICS = 

BIBS = chorus.bib



## You shouldn't have to make any changes below this line.

TEXSRC = ${START}.tex ${MACROS} ${TEXTS} ${TEXPICS} ${PICS} ${PREAMBLE}


# Rules for compiling the main file (usually 0.tex)
${START}.pdf: ${TEXSRC} ${START}.bbl
	pdflatex ${START}
	-grep -s "Rerun " ${START}.log && pdflatex ${START}
	-grep -s "Rerun " ${START}.log && pdflatex ${START}
	-grep -s "Rerun " ${START}.log && pdflatex ${START}


${START}.ps: ${TEXSRC} ${START}.bbl
	latex ${START}
	-grep -s "Rerun " ${START}.log && latex ${START}
	-grep -s "Rerun " ${START}.log && latex ${START}
	-grep -s "Rerun " ${START}.log && latex ${START}
	dvips -o ${START}.ps ${START}.dvi


# .fig.eps:  
## (from http://xpt.sourceforge.net/techdocs/Latex/MakefileForTeX/Latex08.000.html)

#         fig2dev -L ps $(@:.eps=.fig) > $(@:.eps=.ps)
#         ps2epsi $(@:.eps=.ps) $@
#         rm -f $(@:.eps=.ps)



# Rules for compiling LaTeX pictures into EPS and PDF

# TODO: Using mylatex would be particularly interesting for these files.
# But (a) we would have to guarantee that these sources have the same
# preamble as the main file, (b) we should reuse the rule we use to
# compile the main file.
#${MACROS}:
#${TEXEPSPICS}:  # noetig?

%.tex: %.dc
	perl draw-clls-graph.pl $< > $@

# same as pstex_t above!
%.dvi: %.tex ${PREAMBLE} ${MACROS}
	echo "\input preamble" > tmp.tex
	echo "\usepackage{pslatex}" >> tmp.tex
	echo "\thispagestyle{empty}" >> tmp.tex
	echo "\begin{document}" >> tmp.tex
	echo "\input $<" >> tmp.tex
	echo "\end{document}" >> tmp.tex
	latex tmp
	mv tmp.dvi $@
	rm tmp.*

# Now MPI-stype FIG (export from xfig as combined LaTeX + Postscript)
# This needs to build EPS directly to give it priority over
# the fig->eps rule below.
%.eps: %.pstex_t  ${PREAMBLE} ${MACROS}
	echo "\input preamble" > tmp.tex
	echo "\usepackage{pslatex}" >> tmp.tex
	echo "\thispagestyle{empty}" >> tmp.tex
	echo "\begin{document}" >> tmp.tex
	echo "\input $<" >> tmp.tex
	echo "\end{document}" >> tmp.tex
	latex tmp
	cp tmp.dvi test.dvi
	mv tmp.dvi $(subst .pstex_t,.dvi,$@)
	rm tmp.*
	dvips -E -o $@ $(subst .pstex_t,.dvi,$@)

%.eps: %.dvi
	dvips -E -o $@ $<

%.pdf: %.eps
	epstopdf $<






# Rules for compiling FIG into EPS and PDF

%.eps: %.fig
	fig2dev -L eps $< $@

%.pdf: %.fig
	fig2dev -L pdf $< $@

%.eps: %.gif
	convert $< $@

%.eps: %.png
	convert $< $@




# Rules for bibtex. The first rule applies whenever the .bbl file isn't
# up to date (because a .bib has been changed). It runs a recursive `make'
# to ensure that 0.aux exists and is up to date. It would be nice if
# this could be done in a dependency, but this doesn't work here because
# it would run bibtex every time because 0.aux keeps changing along the way.
${START}.bbl: ${BIBS}
	make ${START}.aux
	bibtex ${START}

${START}.aux: ${TEXSRC}
	pdflatex ${START}


index: 
	touch 0.tex
	make
	make ${START}.ind
	make
	make ${START}.ind
	make
	make

${START}.ind: ${START}.idx
	makeindex ${START}


# various utilities

clean:
	rm -f *.aux *.log *.bbl *.blg *.dvi *.ps *.toc *.fmt *.html *.css

pack:
	tar czf ${PROJECT}.tar.gz ${TEXSRC} ${BIBS} Makefile

# copy cmdfilter.pl to current directory if you want this
cmdfilter:
	perl cmdfilter.pl -p ${AUTHORS} -o all.tex ${START}.tex

# copy countall.pl and strip-latex.pl to current dir if you want this
#count:
#	perl countall.pl ${TEXTS}



.PHONY: cmdfilter count pack clean count index



count:
	wc $(TEXTS)





############### HEVEA support #####################

# also type: export HEVEADIR=c:/Programme/hevea


#HEVEAHOME=c:/Programme/hevea
#HEVEADIR=${HEVEAHOME}

HEVEA=hevea
HACHA=hacha
SCP=scp

index.html: ${START}.html
	${HACHA} $<

${START}.html: ${TEXSRC} ${START}.bbl 
	${HEVEA} png.hva $< 

distribute-html: index.html
	${SCP} *.html *.png *.gif *.css koller@httpd.coli.uni-saarland.de:/web/www.coli.uni-saarland.de/html/projects/chorus/utool/manual