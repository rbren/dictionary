package info.bbrennan.dictionary;

import info.bbrennan.dictionary.JWNLUtils.SynsetPointer;
import info.bbrennan.dictionary.JWNLUtils.DefinitionPointer;


import com.google.common.collect.ImmutableList;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import net.didion.jwnl.JWNL;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.IndexWord;
import net.didion.jwnl.data.IndexWordSet;
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

public class AcyclicDictionary {

  private static final List<POS> POS_TYPES = ImmutableList.of(
    POS.NOUN, POS.VERB, POS.ADJECTIVE, POS.ADVERB
  );

  private static final SynsetLocComparator LOC_COMPARATOR = new SynsetLocComparator();

  private Set<Synset> mDefinedSynsets = new HashSet<Synset>();
  private List<SynsetCount> mSynsetCounts = new ArrayList<SynsetCount>(117659);
  private List<StringCount> mStringCounts = new ArrayList<StringCount>(100000);

  public static void main(String[] args) {
    System.out.println("Running Dict");
    try {
      AcyclicDictionary dict = new AcyclicDictionary();
      dict.initializeSynsetHeaps();
      dict.printBestSynsets();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public AcyclicDictionary() {
    try {
      JWNL.initialize(new FileInputStream("props.xml"));
    } catch (IOException e) {

    } catch (JWNLException e) {

    }
  }

  public void initializeSynsetHeaps() throws JWNLException {
    System.out.println("initializing");
    addPosToSynsetHeap(POS.NOUN);
    System.out.println("done with nouns");
    addPosToSynsetHeap(POS.VERB);
    System.out.println("done with verbs");
    addPosToSynsetHeap(POS.ADJECTIVE);
    System.out.println("done with adjs");
    addPosToSynsetHeap(POS.ADVERB);
    System.out.println("done with advbs");

    Collections.sort(mSynsetCounts, LOC_COMPARATOR);

    System.out.println("found synsets:" + mSynsetCounts.size());

    for (POS pos : POS_TYPES) {
      System.out.println("adding for pos:" + pos);
      addCountsForPos(pos);
    }
    Collections.sort(mSynsetCounts, new SynsetCountComparator());
  }

  public void addPosToSynsetHeap(POS pos) throws JWNLException {
    Iterator iter = Dictionary.getInstance().getSynsetIterator(pos);
    while (iter.hasNext()) {
      Synset synset = (Synset) iter.next();
      SynsetCount count = new SynsetCount(synset);
      mSynsetCounts.add(count);
    }
  }

  public void addCountsForPos(POS pos) throws JWNLException {
    Iterator iter = Dictionary.getInstance().getSynsetIterator(pos);
    while (iter.hasNext()) {
      final Synset synset = (Synset) iter.next();
      new Thread(new Runnable() {
        @Override
        public void run() {
          DefinitionPointer def = new DefinitionPointer(synset);
          System.out.print(".");
          for (String word : def.dependencies) {
            StringCount count = new StringCount(word);
            if (!mWordCounts.contains(count)) {
              count.increment();
              mWordCounts.add(count);
            } else {
              mWordCounts.get(mWordCounts.indexOf(count)).increment();
            }
            try {
              //addCountsForWord(word);

            } catch (JWNLException e) {

            }
          }
        }
      }).run();
    }
  }

  public void addCountsForWord(String str) throws JWNLException {
    IndexWordSet wordSet = Dictionary.getInstance().lookupAllIndexWords(str);
    for (POS pos : POS_TYPES) {
      if (wordSet.isValidPOS(pos)) {
        IndexWord iWord = wordSet.getIndexWord(pos);
        Synset[] senses = iWord.getSenses();
        for (int i = 0; i < senses.length; ++i) {
          Synset sense = senses[i];
          int index = Collections.binarySearch(mSynsetCounts, new SynsetCount(sense), LOC_COMPARATOR);
          synchronized(mSynsetCounts) {
            mSynsetCounts.get(index).increment();
          }
        }
      }
    }
  }

  public void printBestSynsets() throws JWNLException {
    System.out.println("Best sets:");
    int i = 0;
    for (SynsetCount synCount : mSynsetCounts) {
      ++i;
      if (i == 20) {break;}
      System.out.println("set:" + synCount.count + ":" + synCount.synset.get().getGloss());
    }
  }
}
