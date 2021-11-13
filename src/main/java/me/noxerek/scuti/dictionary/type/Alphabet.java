package me.noxerek.scuti.dictionary.type;

import me.noxerek.scuti.dictionary.Dictionary;

import java.util.ArrayList;
import java.util.List;

/**
 * @author netindev
 */
public class Alphabet implements Dictionary {

    private final List<String> cachedMixedCaseNames = new ArrayList<>();
    private int index = 0;

    @Override
    public void reset() {
        this.index = 0;
    }

    @Override
    public String next() {
        return this.name(this.index++);
    }

    private String name(final int index) {
        final List<String> cachedNames = this.cachedMixedCaseNames;
        if (index < cachedNames.size()) {
            return cachedNames.get(index);
        }
        final String name = this.newName(index);
        cachedNames.add(index, name);
        return name;
    }

    private String newName(final int index) {
        final int totalCharacterCount = 2 * 26;
        final int baseIndex = index / totalCharacterCount;
        final int offset = index % totalCharacterCount;
        final char newChar = this.charAt(offset);
        return baseIndex == 0 ? String.valueOf(newChar) : this.name(baseIndex - 1) + newChar;
    }

    private char charAt(final int index) {
        return (char) ((index < 26 ? 'a' : 'A' - 26) + index);
    }

}
