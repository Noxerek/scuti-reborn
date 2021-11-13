package me.noxerek.scuti.dictionary.type;

import me.noxerek.scuti.dictionary.Dictionary;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @author netindev
 */
public class Custom implements Dictionary {

    private final List<String> lines;
    private final Alphabet alphabet;

    private final String[] types = {"[", "]", ";", ".", "(", ")"};

    private int count;

    /* init */ {
        this.lines = new ArrayList<>();
        this.alphabet = new Alphabet();
    }

    public Custom(final File file) {
        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            String string;
            while ((string = reader.readLine()) != null) {
                if (!string.isEmpty() && this.isTypeSafe(string) && string.length() < Short.MAX_VALUE) {
                    this.lines.add(string);
                }
            }
            reader.close();
        } catch (final Exception e) {
            throw new RuntimeException("Error while trying to read: \"" + file + "\" file");
        }
    }

    @Override
    public String next() {
        return this.lines.size() != this.count ? this.lines.get(this.count++) : this.alphabet.next();
    }

    @Override
    public void reset() {
        this.count = 0;
        this.alphabet.reset();
    }

    boolean isTypeSafe(final String string) {
        for (final String type : this.types) {
            if (string.contains(type)) {
                return false;
            }
        }
        return true;
    }

}
