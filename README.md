## Super CSV File ##

**Super CSV File** is a full-featured tool to analyze CSV file.

### Features of the tool ###

The tool will provide you with some properties of CSV file that you can use in other application:

- **compute the maximum size of each field along the file** (`--columnSize`)
- Find the delimiter (even non standat one) (`--delimiter`)
- Extract lines from the CSV file (`--extract`)
- Determine the encoding of the file like UTF-8 (`--encoding`)

### Compute the size of each field ###

Sometimes you need to know the maximum size of each field, in particular for some size optimization during an import of the CSV file in a structured database. This program will provide you with the exact information for that purpose. Because the application is fully multithread, even a 50Gb CSV file will be analyzed in a few minutes on a recent computer.

Right know, there is no special treatment for the quote character. So this character will be count in the size of the field. This feature will come soon.

### Find the delimiter ###

As you may know, in a CSV file, each field on one line is separated with a delimiter. People use to not restrict themselves in the choice of the delimiter. The only thing is that people don't use alphanumeric characters for that purpose. This program can analyze the first 1 000 lines of the CSV file to find the character used as a delimiter. This information may be used for the importation in your structured database 

### Determine the encoding ###

This application can be used to determine the encoding of a file. This can be useful as with a bad encoding the data from the CSV file may be corrupted.

### Help screen (to update) ###

    Super Tax Lawyer is a program to play with accounting exported as text files.

      --columnCount  <arg>     [OPTIONAL] Number of columns expected.
  -c, --columnSize  <arg>      Print the detected encoding of each file provided.
  -d, --debug                  Display lots of debug information during the
                               process.
      --no-debug               Display minimum during the process (same as not
                               using this argument).
  -e, --excludeTitles          Exclude titles of columns in column size result.
      --no-excludeTitles
  -f, --forceEncoding  <arg>   Force the encoding of the text file.
  -o, --outputFolder  <arg>    Path to the folder where to save the results.
  -s, --splitter  <arg>        Character used to split a line in columns. Use TAB
                               for tabulation and SPACE for space separators.
  -h, --help                   Show this message.
