package component;

import java.io.*;

/**
 *   IOManager: managing file input and output based on specified filename from Terminal.
 */
public class IOManager {
    public String filename;

    public IOManager(String filename) {
        this.filename = filename;
    }

    public String getFilename() {
        return filename;
    }

    /**
     *  inner class Reader should first check validity of given filename, then support method for read one character each time.
     */
    public class Reader {
        private BufferedReader reader;

        public Reader() throws IOException {
            File file = new File(filename);
            if (file.isDirectory()) {
                throw new RuntimeException("Unable to open file nameThatIsADirectory.");
            }
            if (!file.exists()) {
                boolean res = file.createNewFile();
                if (!res) {
                    throw new RuntimeException("Failed to create a new file.");
                }
            }
            reader = new BufferedReader(new FileReader(file));
        }

        public char getNextCharacter() throws IOException {
            int c;
            if ((c = reader.read()) != -1) {
                if ((char)c == '\r') {
                    c = reader.read();
                }
                return (char)c;
            }
            reader.close();
            return (char)-1;
        }
    }

    /**
     *   inner class Write should write content currently maintained by TextBuffer instance to the given path one character by one character.
     */
    public class Writer {
        private BufferedWriter writer;

        public Writer() throws IOException {
            writer = new BufferedWriter(new FileWriter(new File(filename)));
        }

        public void writeNextCharacter(char c) throws IOException {
            writer.write(c);
        }

        public void close() throws IOException {
            writer.close();
        }
    }
}
