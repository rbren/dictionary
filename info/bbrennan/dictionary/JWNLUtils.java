package info.bbrennan.dictionary;

import com.google.common.base.Splitter;

import java.util.List;

import net.didion.jwnl.JWNL;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.IndexWord;
import net.didion.jwnl.data.POS;
import net.didion.jwnl.data.PointerType;
import net.didion.jwnl.data.PointerUtils;
import net.didion.jwnl.data.Synset;
import net.didion.jwnl.data.Word;
import net.didion.jwnl.data.list.PointerTargetNodeList;
import net.didion.jwnl.data.list.PointerTargetTree;
import net.didion.jwnl.data.relationship.AsymmetricRelationship;
import net.didion.jwnl.data.relationship.Relationship;
import net.didion.jwnl.data.relationship.RelationshipFinder;
import net.didion.jwnl.data.relationship.RelationshipList;
import net.didion.jwnl.dictionary.Dictionary;

public class JWNLUtils {
  private JWNLUtils() {}

  public static Iterable<String> getWordsFromSynsetDefinition(Synset synset) {
    String gloss = synset.getGloss();
    int idx = gloss.indexOf(";");
    String defn = idx < 0 ? gloss : gloss.substring(0, idx);
    return Splitter.on(' ').split(defn);
  }

  public static class SynsetPointer {
    public long loc;
    public POS pos;

    public SynsetPointer(Synset synset) {
      pos = synset.getPOS();
      loc = synset.getOffset();
    }

    public Synset get() throws JWNLException {
      return Dictionary.getInstance().getSynsetAt(pos, loc);
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof SynsetPointer)) {
        return false;
      }
      SynsetPointer other = (SynsetPointer) obj;
      return other.pos.equals(pos) && other.loc == loc;
    }
  }

  public static class DefinitionPointer {
    public Iterable<String> dependencies;

    public DefinitionPointer(Synset synset) {
      dependencies = getWordsFromSynsetDefinition(synset);
    }
  }

  public static class StringCount {
    public final String str;
    public int count = 0;

    public StringCount(String str) {
      this.str = str;
    }

    public void increment() {
      ++count;
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof StringCount && ((StringCount) other).str.equals(synset);
    }
  }

  private class SynsetCount {
    public final SynsetPointer synset;
    public int count;

    public SynsetCount(Synset synset) {
      this.synset = new SynsetPointer(synset);
      this.count = 0;
    }

    public void increment() {
      ++count;
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof SynsetCount && ((SynsetCount) other).synset.equals(synset);
    }
  }

  private static class SynsetCountComparator implements Comparator<SynsetCount> {
    @Override
    public int compare(SynsetCount first, SynsetCount second) {
      return second.count - first.count;
    }
  }

  private static class StringCountComparator implements Comparator<StringCount> {
    @Override
    public int compare(StringCount first, StringCount second) {
      return second.count - first.count;
    }
  }

  private static class SynsetLocComparator implements Comparator<SynsetCount> {
    @Override
    public int compare(SynsetCount first, SynsetCount second) {
      int firstPos = POS_TYPES.indexOf(first.synset.pos);
      int secondPos = POS_TYPES.indexOf(second.synset.pos);
      if (firstPos != secondPos) {
        return firstPos - secondPos;
      }

      if (second.synset.loc == first.synset.loc) {
        return 0;
      }
      return second.synset.loc > first.synset.loc ? 1 : -1;
    }
  }
}
