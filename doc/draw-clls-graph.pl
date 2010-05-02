# This Perl program generates a pstricks-macro Latex file representing a
# CLLS graph from a human-readable input file.
#
# The basic building block of the input file is a line of the form
#
#   f(a,g(h(b)))
#
# This will generate Latex code displaying this tree. The symbols in the
# term can have the following specialties:
#
#  f<X|Y>       give the node the (visible) variable label Y, and pstricks nodename X
#               special case:  f<X> only assigns a node name
#
#  [f]          generate \Word{f}
#  ~            symbols in the label are replaced by spaces
#  .            is the special label for an empty node without label
#
# Empty nodes (with label ".") can still have subterms in the input language.
# These subterms are rendered as children of the empty node via dominance edges.
#
# Overly long lines can be continued on the next line by putting a backslash
# character at the end of each line except for the last.
#
# Trees can be structured using <TreeList> ... </TreeList> commands and
# <SpaceNode>s. etc etc
#
# Lines starting with # are comments. Lines starting with ! are taken over
# into the generated file verbatim (without the leading !).
#
# Alexander Koller, March 2004
#






$gensym_counter = 1;


sub gensym {
    return "DCG" . ($gensym_counter++);
}




my $recDescStr;
my $indent = 0;


sub indentStr() {
    return ' ' x $indent;
}


# Invariant: After a call instance returns, it has removed its complete
# subterm (including the closing bracket) from the global variable $recDescStr.
sub recDesc {
    my $keepLooping = 1;
    my $head;
    my ($label, $varlabel, $varname);
    my $isLeaf = 1;
    my $isDomParent = 0;
    

    ## strip off head
    $recDescStr =~ /^\s*([^\(\), ]+)\s*(.*)/;
    $head = $1;
    $recDescStr = $2;

    if( $recDescStr =~ s/^\((.*)/$1/ ) {
	$isLeaf = 0;
    }



    ## analyse head
    if( $head =~ /^([^<]+)<([^|>]*)\|([^>]*)>/ ) {  # f<X|Y>
	$label = $1;
	$varlabel = $3;
	$varname = $2;
    } elsif( $head =~ /^([^<]+)<([^|>]*)>/ ) {      # f<X>
	$label = $1;
	$varlabel = "";
	$varname = $2;
    } else {                                        # f
	$label = $head;
	$varlabel = "";
	$varname = "";
    }

    if( $varname eq "" ) {
	$varname = gensym();
    }

    # special labels
    if( $label eq '[lam]') {
	$label = '\lam';
    } elsif( $label eq '[var]') {
	$label = '\var';
    } elsif( $label eq '.') {
	$label = '';
	$isDomParent = 1;
    } elsif( $label =~ /^\[([^\]]+)/ ) {          # [f]
	# \Word label
	$label = "\\Word{$1}";
    }

    # ~ is interpreted as space marker, as in \exists~x
    $label =~ s/~/ /g;





    ## print Latex code for head 

    print indentStr();

    if( $isLeaf ) {
	print "\\Node{$label}{$varlabel}{$varname}\n";
    } else {
 	if( $isDomParent ) {
	    print "\\DomTree{$varlabel}{$varname}{\n";
	} else {
	    print "\\LabTree{$label}{$varlabel}{$varname}{\n";
	}


	## recursion over subterms
	$indent += 3;
	    
	while( $keepLooping ) {
	    recDesc();
	    
	    # If the next symbol is a comma, stay in the loop for the
	    # next subterm.
	    if( $recDescStr =~ /^\s*,(.*)/ ) {
		$recDescStr = $1;
	    } else {
		$keepLooping = 0;
	    }
	}
	
	# Strip off closing bracket
	$recDescStr =~ s/^\s*\)(.*)/$1/;
	    
	
	$indent -= 3;
	
	print indentStr() . "}\n";
    }
}







%specialmacros = ('SpaceNode' => '\SpaceNode',
		  'DomEdge' => '\domedge',
		  'TreeEdge' => '\solidedge',
		  'Strut' => '\strut',
		  'WhiteX' => '{\white x}',
		  'LambdaLink' => '\LambdaLink',
		  'AnaLink' => '\AnaLink'
		  );



while(<>) {
    chomp;

    # # indicates a comment line
    if( /^\s*\#/ ) {
    }

    # ! indicates a verbatim line
    elsif( /!(.*)/ ) {
	print "$1\n";
    }

    # <TreeList> ... </TreeList> generates \TreeList constructions
    elsif( /<TreeList>/ ) {
	print "\n\\TreeList{\n\n";
    }

    elsif( /<\/TreeList>/ ) {
	print "}\n\n";
    }

    # special commands
    # TODO: check for correct number of arguments.
    elsif( /<\s*(\S+)\s*(.*?)\s*\/>/ ) {
	my $macro = $specialmacros{$1};
	my @arguments = split /\s+/, $2;

	print $macro;
	if( $#arguments >= 0 ) {
	    print "{" . join('}{', @arguments) . "}";
	}
	print "\n";
    }

    # lines ending in \ are remembered for next time
    elsif( /^(.*)\\\s*$/ ) {
	$recDescStr .= $1;
    }
	
    # empty lines are ignored, everything else is processed
    elsif( !/^\s*$/ ) {
	print "% $_\n";
	$recDescStr .= $_;
	recDesc();

	print "\n\n";
    }
}
