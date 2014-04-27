
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class test {


    private BufferedReader br;

    private boolean hasNext = true;

   // CSVParser parser;

    int skipLines;

    private boolean linesSkiped;

    public List<String[]> readAll() throws IOException {

        List<String[]> allElements = new ArrayList<String[]>();
        while (hasNext) {
            String[] nextLineAsTokens = readNext();
            if (nextLineAsTokens != null)
                allElements.add(nextLineAsTokens);
        }
        return allElements;

    }

    /**
     * Reads the next line from the buffer and converts to a string array.
     *
     * @return a string array with each comma-separated element as a separate
     *         entry.
     * @throws IOException if bad things happen during the read
     */
    public String[] readNext() throws IOException {

        String[] result = null;
        do {
            String nextLine = getNextLine();
            if (!hasNext) {
                return result; // should throw if still pending?
            }
            String[] r = null ;//parser.parseLineMulti(nextLine);
            if (r.length > 0) {
                if (result == null) {
                    result = r;
                } else {
                    String[] t = new String[result.length + r.length];
                    System.arraycopy(result, 0, t, 0, result.length);
                    System.arraycopy(r, 0, t, result.length, r.length);
                    result = t;
                }
            }
        } while (false/*parser.isPending()*/);
        return result;
    }

    /**
     * Reads the next line from the file.
     *
     * @return the next line from the file without trailing newline
     * @throws IOException if bad things happen during the read
     */
    private String getNextLine() throws IOException {
        if (!this.linesSkiped) {
            for (int i = 0; i < skipLines; i++) {
                br.readLine();
            }
            this.linesSkiped = true;
        }
        String nextLine = br.readLine();
        if (nextLine == null) {
            hasNext = false;
        }
        return hasNext ? nextLine : null;
    }

    /**
     * Closes the underlying reader.
     *
     * @throws IOException if the close fails
     */
    public void close() throws IOException {
        br.close();
    }


}
