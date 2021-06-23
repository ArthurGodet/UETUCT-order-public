/*
@author Arthur Godet <arth.godet@gmail.com>
@since 17/01/2020
*/
package data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import gnu.trove.list.array.TIntArrayList;
import java.io.File;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

public class InstanceUETUCT {
    @JsonIgnore
    private int size;
    private final String name;
    private final int n;
    private final int m;
    @JsonIgnore
    private final int[][] pred;
    private final int[][] succ;
    @JsonIgnore
    private final int[][] ancestors;
    @JsonIgnore
    private final int[][] descendants;

    @JsonCreator
    public InstanceUETUCT(@JsonProperty("name") String name,
        @JsonProperty("n") int n,
        @JsonProperty("m") int m,
        @JsonProperty("succ") int[][] succ,
        @JsonProperty("size") int size) {
        this.size = size;
        this.name = name;
        this.n = n;
        this.m = m;
        this.succ = succ;
        pred = new int[n][];
        TIntArrayList list = new TIntArrayList(n);
        for(int i = 0; i < n; i++) {
            list.clear();
            for(int j = i-1; j >= 0; j--) {
                if(Factory.contains(succ[j], i)) {
                    list.add(j);
                }
            }
            pred[i] = Arrays.stream(list.toArray()).sorted().toArray();
        }
        if(!correctData(n, pred, succ)) {
            throw new UnsupportedOperationException("Instance not correct");
        }
        this.ancestors = buildAncestors();
        this.descendants = buildDescendants();
    }

    public static boolean correctData(int n, int[][] pred, int[][] succ) {
        if(n != pred.length || n != succ.length) {
            return false;
        }
        for(int i = 0; i<n; i++) {
            for(int j = 0; j<pred[i].length; j++) {
                if(!Factory.contains(succ[pred[i][j]], i)) {
                    return false;
                }
            }
        }
        for(int i = 0; i<n; i++) {
            for(int j = 0; j<succ[i].length; j++) {
                if(!Factory.contains(pred[succ[i][j]], i)) {
                    return false;
                }
            }
        }
        // TODO : check that there is no cycle
        return true;
    }

    public String getName() {
        return name;
    }

    public int getN() {
        return n;
    }

    public int getM() {
        return m;
    }

    public int[][] getPred() {
        return pred;
    }

    public int[][] getSucc() {
        return succ;
    }

    public int[][] getAncestors() {
        return ancestors;
    }

    public int[][] getDescendants() {
        return descendants;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof InstanceUETUCT) {
            InstanceUETUCT inst = (InstanceUETUCT) o;
            if(inst.n == this.n && inst.m == this.m) {
                for(int i = 0; i<n; i++) {
                    if(inst.pred[i].length != this.pred[i].length) {
                        return false;
                    }
                    for(int j = 0; j<this.pred[i].length; j++) {
                        if(inst.pred[i][j] != this.pred[i][j]) {
                            return false;
                        }
                    }
                }
                for(int i = 0; i<n; i++) {
                    if(inst.succ[i].length != this.succ[i].length) {
                        return false;
                    }
                    for(int j = 0; j<this.succ[i].length; j++) {
                        if(inst.succ[i][j] != this.succ[i][j]) {
                            return false;
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }

    public int longestPath(int from, int to) {
        if(from == to) {
            return 0;
        } else if(succ[from].length == 0) {
            return -1;
        }
        int max = -1;
        for(int k = 0; k<succ[from].length; k++) {
            int lp = longestPath(succ[from][k], to);
            max = Math.max(max, lp);
        }
        return (max == -1 ? -1 : max+1);
    }

    public int[] deepestSuccessor() {
        int[] deepestSuccessor = new int[succ.length];
        int n = succ.length;
        LinkedList<Integer> list = new LinkedList<>();
        boolean[] hasBeenTreated = new boolean[n];
        for(int i = 0; i < n; i++) {
            if(succ[i].length == 0) {
                list.add(i);
            }
        }
        while(!list.isEmpty()) {
            int i = list.removeFirst();
            boolean allSuccTreated = true;
            for(int j = 0; j < succ[i].length && allSuccTreated; j++) {
                allSuccTreated = hasBeenTreated[succ[i][j]];
            }
            if(allSuccTreated) {
                for(int j = 0 ; j < succ[i].length; j++) {
                    deepestSuccessor[i] = Math.max(deepestSuccessor[i], 1 + deepestSuccessor[succ[i][j]]);
                }
                hasBeenTreated[i] = true;
                for(int j = 0; j < pred[i].length; j++) {
                    if(!list.contains(pred[i][j])) {
                        list.addLast(pred[i][j]);
                    }
                }
            }
        }
        return deepestSuccessor;
    }

    public int[] depth() {
        int[] depth = new int[n];
        for(int i = 0; i<depth.length; i++) {
            for(int k = 0; k<pred[i].length; k++) {
                depth[i] = Math.max(depth[i], 1 + depth[pred[i][k]]);
            }
        }
        return depth;
    }

    public void reduce() {
        int[] depth = depth();
        TIntArrayList list = new TIntArrayList(n);
        for(int i = 0; i<n; i++) {
            list.clear();
            int[] suci = this.succ[i];
            for(int j = 0; j<suci.length; j++) {
                if(depth[suci[j]] - depth[i] == 1) {
                    list.add(suci[j]);
                }
            }
            this.succ[i] = Arrays.stream(list.toArray()).sorted().toArray();
        }

        for(int i = 0; i<n; i++) {
            list.clear();
            for(int j = i-1; j>=0; j--) {
                if(Factory.contains(succ[j], i)) {
                    list.add(j);
                }
            }
            pred[i] = Arrays.stream(list.toArray()).sorted().toArray();
        }
    }

    private int[][] buildAncestors() {
        int n = pred.length;
        int[][] ancestors = new int[n][];
        HashSet<Integer>[] sets = new HashSet[n];
        BitSet bitSet = new BitSet(n);
        boolean[] done = new boolean[n];
        for(int i = 0; i < n; i++) {
            sets[i] = new HashSet<>(n);
            if(pred[i].length == 0) {
                bitSet.set(i);
            }
        }
        while(!bitSet.isEmpty()) {
            int i = bitSet.nextSetBit(0);
            bitSet.clear(i);
            if(!done[i]) {
                boolean allDone = true;
                for(int j = 0; j < pred[i].length && allDone; j++) {
                    allDone = done[pred[i][j]];
                }
                if(allDone) {
                    for(int j = 0; j < pred[i].length; j++) {
                        sets[i].add(pred[i][j]);
                        sets[i].addAll(sets[pred[i][j]]);
                    }
                    for(int j = 0; j < succ[i].length; j++) {
                        bitSet.set(succ[i][j]);
                    }
                    done[i] = true;
                }
            }
        }
        for(int i = 0; i < n; i++) {
            ancestors[i] = new int[sets[i].size()];
            Iterator<Integer> iter = sets[i].iterator();
            int j = 0;
            while(iter.hasNext()) {
                ancestors[i][j++] = iter.next();
            }
        }
        return ancestors;
    }

    private int[][] buildDescendants() {
        int n = succ.length;
        int[][] descendants = new int[n][];
        HashSet<Integer>[] sets = new HashSet[n];
        BitSet bitSet = new BitSet(n);
        boolean[] done = new boolean[n];
        for(int i = 0; i < n; i++) {
            sets[i] = new HashSet<>();
            if(succ[i].length == 0) {
                bitSet.set(i);
            }
        }
        while(!bitSet.isEmpty()) {
            int i = bitSet.nextSetBit(0);
            bitSet.clear(i);
            if(!done[i]) {
                boolean allDone = true;
                for(int j = 0; j < succ[i].length && allDone; j++) {
                    allDone = done[succ[i][j]];
                }
                if(allDone) {
                    for(int j = 0; j < succ[i].length; j++) {
                        sets[i].add(succ[i][j]);
                        sets[i].addAll(sets[succ[i][j]]);
                    }
                    for(int j = 0; j < pred[i].length; j++) {
                        bitSet.set(pred[i][j]);
                    }
                    done[i] = true;
                }
            }
        }
        for(int i = 0; i < n; i++) {
            descendants[i] = new int[sets[i].size()];
            Iterator<Integer> iter = sets[i].iterator();
            int j = 0;
            while(iter.hasNext()) {
                descendants[i][j++] = iter.next();
            }
        }
        return descendants;
    }

    public static void main(String[] args) {
        File[] files = Factory.listAllFiles("data/", ".json", true);
        for(File file : files) {
            InstanceUETUCT inst = Factory.fromFile(file.getAbsolutePath(), InstanceUETUCT.class);
            Factory.toFile(file.getAbsolutePath(), inst);
        }
    }
}
