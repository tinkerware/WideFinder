package widefinder

String text    = new File( "c:/Projects/WideFinder/data/data-100000.log" ).text;
File   newFile = new File( "c:/Projects/WideFinder/data/O.Big3.log" );

for( i in 1..10 ){ newFile.append( text ); println i }
