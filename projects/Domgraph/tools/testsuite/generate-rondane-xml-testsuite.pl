# This script generates an XML testsuite from a collection
# of USRs.  This testsuite can then be used e.g. in the context
# of the RondaneTestsuite to make sure that Utool still behaves
# like it did when this script was run.
#
# To generate the XML file, change to a directory that has
# USRs in individual files, and run "perl generate-testsuite.pl <tsname>".
# This will generate a file "tsname.xml" in the current directory
# containing one testcase for each USR file.  When generating an
# MRS-based testsuite, make sure that the directory also contains
# non-nets.
#
# Currently the script expects files with extension .mrs.pl
# encoded in mrs-prolog, but this can be changed easily.

use IO::Socket;

sub xmlify {
  my $usr = shift;

    # encode entities
    $usr =~ s[%.*][]g;
    $usr =~ s[\n][ ]g;
    $usr =~ s[&][&amp;]sg;
    $usr =~ s["][&quot;]sg;  #"
    $usr =~ s['][&apos;]sg;  #' 
    $usr =~ s[<][&lt;]sg;
    $usr =~ s[>][&gt;]sg;

  return $usr;
}
  

# return 1 iff conversion was successful
sub convert {
    my $id = shift;
    my $codec = shift;
    my $usr = shift;

    my $socket = IO::Socket::INET->new("localhost:2802")
    or die $!;
    
    print $socket "<utool cmd='convert' output-codec='domcon-oz'>\n";
    print $socket "  <usr codec='$codec' string='$usr' />\n";
    print $socket "</utool>\n";
    $socket->shutdown(1);

    my $answer = join(' ', <$socket>);
    close $socket;
    
    if( $answer =~ /<error/ ) {
      $answer =~ /code=.([^"']*)/;
      print OUT "    <domgraph error='$1' />\n";
      return 0;
    } else {
      $answer =~ /usr=.([^"']*)/;
      print OUT "    <domgraph string='$1' />\n";
      return 1;
    }
  }


sub classify {
    my $id = shift;
    my $codec = shift;
    my $usr = shift;

    my $socket = IO::Socket::INET->new("localhost:2802")
    or die $!;
    
    print $socket "<utool cmd='classify'>\n";
    print $socket "  <usr codec='$codec' string='$usr' />\n";
    print $socket "</utool>\n";
    $socket->shutdown(1);

    my $answer = join(' ', <$socket>);
    close $socket;
  
    if( $answer =~ /code=.([^"']*)/ ) {
      print OUT "    <classify code='$1' />\n";
    }
}


sub solve {
    my $id = shift;
    my $codec = shift;
    my $usr = shift;

    my $socket = IO::Socket::INET->new("localhost:2802")
    or die $!;
    
    print $socket "<utool cmd='solvable'>\n";
    print $socket "  <usr codec='$codec' string='$usr' />\n";
    print $socket "</utool>\n";
    $socket->shutdown(1);

    my $answer = join(' ', <$socket>);
    close $socket;

    if( $answer =~ /solvable=.true/ ) {
      ($count) = ($answer =~ /count=.([^"']*)/);
      ($chartsize) = ($answer =~ /chartsize=.([^"']*)/);
      print OUT "    <solve solvable='true' count='$count' chartsize='$chartsize' />\n";
    } else {
      print OUT "    <solve solvable='false' />\n";
    }
  }





################# MAIN ######################

$testsuitename = $ARGV[0];
$codec = 'mrs-prolog';
$time = localtime();

opendir(DIR, ".") || die "$!";
@mrsfiles = grep { /\.mrs\.pl$/ } readdir(DIR);
closedir DIR;

open OUT, ">$testsuitename.xml" or die "$!";

print OUT <<"END";
<?xml version='1.0' ?>

<testsuite name='$testsuitename' generated-at='$time'>
END


for $filename (@mrsfiles) {
    print STDERR "Processing $filename ... \n";

    ($sentid) = ($filename =~ /^(\d+)/);

    open MRSFILE, $filename or die "$!";
    $usr = xmlify(join('', <MRSFILE>));
    close MRSFILE;

    print OUT "  <usr id='$sentid' codec='$codec' string='$usr'>\n";

    if( convert($sentid, $codec, $usr) ) {
      classify($sentid, $codec, $usr);
      solve($sentid, $codec, $usr);
    }

    print OUT "  </usr>\n";
}

print OUT <<'EOP';
</testsuite>
EOP


    
system "gzip $testsuitename.xml";
