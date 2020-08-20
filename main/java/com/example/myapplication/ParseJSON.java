package com.example.myapplication;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;


import static java.lang.Math.ceil;
import static java.lang.Math.log;

class NameStructure {
    public int level;
    public String parent;
    public Short treeID;
    public NameStructure next;

    public NameStructure(int level, String parent){
        this.level = level;
        this.parent = parent;
        this.treeID = -1;
        this.next = null;
    }

    public boolean noTree(){
        return treeID == -1;
    }

    public void append(int level, String parent){
        NameStructure next = new NameStructure(level, parent);
        this.next = next;
    }

    public NameStructure find(int level, String parent){
        if(this.parent.equals(parent)
                && this.level == level){
            return this;
        }
        else{
            if (this.next == null){
                return null;
            }
            else{
                return this.next.find(level, parent);
            }
        }
    }

    public NameStructure tail(){
        if (this.next == null){
            return this;
        }
        else{
            return this.next.tail();
        }
    }

    public short addIndex(Short treeID, Integer did, Integer hashResult, Map<Short, treeRoot> Forest,
                         Map<Short, treeRoot> DiskForest, Map weight){


        //System.out.println("hashed" + hashResult);

        //if the tree exist

        if (this.noTree()) {
            //new tree and new weight
            this.treeID = treeID;

            treeRoot vTree = new treeRoot();
            vTree.addDid(did,hashResult);
            Forest.put(treeID, vTree);

            treeRoot vTree_disk = new treeRoot();
            vTree_disk.addDid(did,hashResult);
            DiskForest.put(treeID, vTree_disk);

            weight.put(treeID, 0.00001);
            //System.out.println( ojt + "new value tree!!!! did:" + did + "name:" + parent + level + entry.getKey() + "  treeID: "+tempNS.treeID + vTree);
            //treeID++;
            //indexNum++;
            return 1;
        } else {
            treeRoot vTree = Forest.get(this.treeID);
            //System.out.println("add to the existed tree" + vtree);
            vTree.addDid(did, hashResult);

            treeRoot vTree_disk = DiskForest.get(this.treeID);
            vTree_disk.addDid(did, hashResult);
            return 0;
        }

    }
}

class keywordEntity{
    public ArrayList dids;
    public double score;

    public keywordEntity(int did){
        this.dids = new ArrayList();
        this.dids.add(did);
        this.score = 0;
    }

    public void add(int did){
        this.dids.add(did);
        score += 1;
    }

    public void timeDecrease(){
        this.score *= 0.5;
    }

    public void hitIncrease(){
        this.score += 100;
    }
}

class treeRoot{
    TreeMap<Integer, ArrayList> tree;
    int size;

    public treeRoot(){
        TreeMap<Integer, ArrayList> tree = new TreeMap<Integer, ArrayList>();
        this.tree = tree;
        this.size = 0;
    }

    public boolean containsKey(Integer hashResult){
        return this.tree.containsKey(hashResult);
    }

    public void addDid(Integer did, Integer hashResult){
        if(this.tree.containsKey(hashResult)){
            this.tree.get(hashResult).add(did);
        }
        else{
            ArrayList<Integer> dids = new ArrayList<>();
            dids.add(did);
            this.tree.put(hashResult, dids);
        }
        this.size++;
    }

    public void addDidList(Integer hashResult, ArrayList fullList){
        int prev_size = this.tree.get(hashResult).size();
        this.tree.put(hashResult, fullList);
        this.size += (this.tree.get(hashResult).size() - prev_size);
    }

    public int getSize(){
        return size;
    }

    public ArrayList get(Integer hashResult){
        return this.tree.get(hashResult);
    }
}

public class ParseJSON extends AppCompatActivity {

    DatabaseHelper myDB;
    int textNum = 0;
    int indexNum = 0;
    int wordNum = 0;
    int keywordNum = 0;
    int keywordMiss = 0;

    int queryNum = 0;
    int querySuccess = 0;

    int newIndex = 0;
    int reward = 0;
    double difference = 0;
    double qvalue_prev = 0;
    double qvalue_curr = 0;

    Map<String, NameStructure> NameHash;
    Map<Short, treeRoot> Forest;

    Map<Short, treeRoot> DiskForest;


    Map<String, keywordEntity> keywordTable;
    Map<String, keywordEntity> DiskkeywordTable;

    Map<String, Integer> idf;
    Map<Short, Double> weight;
    public double a = 0.000000000005;
    public double y = 0.5;
    public boolean pruning;

    public int did;
    public short treeID;

    public void fillTree(Short key) {
        /*reboost tree from disk*/
        //experiment one
        TreeMap<Integer, ArrayList> diskTemp = DiskForest.get(key).tree;
        for (Map.Entry<Integer, ArrayList> entry : diskTemp.entrySet()) {
            ArrayList tempA = (ArrayList) entry.getValue().clone();
            Forest.get(key).addDidList(entry.getKey(), tempA);
        }
    }

    public int hashFunc(Object ojt) {

        if (ojt == null) {
            return -1;
        } else {
            String value;
            if (ojt instanceof String) {
                value = (String) ojt;
            } else if (ojt instanceof Integer) {
                return (Integer) ojt;
            } else {
                value = ojt.toString();
            }
            return value.hashCode();
        }
    }

    public int treeSize(Short Tid) {
        /*calculate
        int treesize = 0;
        TreeMap<Integer, ArrayList> tempTree = Forest.get(Tid);
        //System.out.println("Tid:"+Tid);
        for (TreeMap.Entry<Integer, ArrayList> en : tempTree.entrySet()) {
            treesize += en.getValue().size();
        }
        //System.out.println("treesize:"+treesize);
        return treesize;*/
        return Forest.get(Tid).getSize();
    }

    public int diskTreeSize(Short Tid) {
        /*
        int treesize = 0;
        TreeMap<Integer, ArrayList> tempTree = DiskForest.get(Tid);
        //System.out.println("Tid:"+Tid);
        for (TreeMap.Entry<Integer, ArrayList> en : tempTree.entrySet()) {
            treesize += en.getValue().size();
        }
        //System.out.println("treesize:"+treesize);
        return treesize;*/
        return DiskForest.get(Tid).getSize();
    }

    public double getNextQ(int action) {
        Map<Short, Integer> tempNum = new HashMap<Short, Integer>();
        Short tempw = 0;
        for (Short i = 0; i < treeID; i++) {
            tempNum.put(i, treeSize(i));
        }

        while (action > 0) {
            tempw = weightMinkey(tempNum);
            if (action > treeSize(tempw)) {
                action -= tempNum.get(tempw);
                tempNum.put(tempw, 0);

            } else {
                tempNum.put(tempw, tempNum.get(tempw) - action);
                action = 0;
            }

        }
        double q = 0;
        for (Short i = 0; i < treeID; i++) {
            q += weight.get(i) * tempNum.get(i);
        }
        return q;
    }

    public short weightMaxkey() {
        Short keyMax = 0;
        double wmax = -10000000;
        Random rand = new Random();
        for (Short i = 1; i < treeID; i++) {
            if (weight.get(i) >= wmax && treeSize(i) < diskTreeSize(i)) {
                wmax = weight.get(i);
                keyMax = i;
            }
        }
        return keyMax;
    }

    public double getQ() {
        double q = 0;
        for (Short i = 0; i < treeID; i++) {
            q += weight.get(i) * treeSize(i);
        }
        return q;
    }

    public short weightMinkey(Map<Short, Integer> Num) {
        Short keyMin = 0;
        double wmin = weight.get((short) 0);
        Random rand = new Random();
        for (Short i = 1; i < treeID; i++) {
            if (weight.get(i) <= wmin && Num.get(i) > 0 && rand.nextBoolean()) {
                wmin = weight.get(i);
                keyMin = i;
            }
        }
        return keyMin;
    }

    public void updateWeight(double difference) {
        for (Short i = 0; i < treeID; i++) {
            weight.put(i, (weight.get(i) + a * difference * treeSize(i)));
        }
    }

    public void keywordExtraction(String text) {
        textNum++;
        String[] words = text.split("\\s+");
        Map<String, Integer> tf = new HashMap<String, Integer>();
        int wordTotal = 0;

        //calculate TF
        for (int i = 0; i < words.length; i++) {
            words[i] = words[i].replaceAll("[^\\w]", "");
            if (words[i] == "") {
                continue;
            }
            if (tf.get(words[i]) != null) {
                tf.put(words[i], tf.get(words[i]) + 1);
            } else {
                tf.put(words[i], 1);
            }
            //System.out.println(words[i]);
            wordTotal++;
        }

        wordNum += wordTotal;
        Map<String, Double> TFIDF = new HashMap<String, Double>();

        //calculate TFIDF
        for (Map.Entry<String, Integer> entry : tf.entrySet()) {
            //System.out.println("IDF");
            if (idf.containsKey(entry.getKey())) {
                idf.put(entry.getKey(), idf.get(entry.getKey()) + entry.getValue());
            } else {
                idf.put(entry.getKey(), entry.getValue());
            }
            //System.out.println("key = " + entry.getKey() + ", value = " + entry.getValue());
            /*System.out.println("TF"+entry.getValue()/(float)wordTotal);
            System.out.println("IDF"+idf.get(entry.getKey())/(float)textNum);*/
            double tfidf = entry.getValue() / (float) wordTotal * log((double) textNum / idf.get(entry.getKey()));
            //System.out.println("tfidf"+tfidf);

            //add to table
            if (did < 5) {
                keywordNum++;
                if (keywordTable.containsKey(entry.getKey())) {
                    keywordTable.get(entry.getKey()).add(did);
                } else {
                    keywordEntity kE = new keywordEntity(did);
                    keywordTable.put(entry.getKey(), kE);

                    indexNum++;
                }

                //disk
                if (DiskkeywordTable.containsKey(entry.getKey())) {
                    DiskkeywordTable.get(entry.getKey()).add(did);
                } else {
                    keywordEntity kE = new keywordEntity(did);
                    DiskkeywordTable.put(entry.getKey(), kE);

                }
            }

            TFIDF.put(entry.getKey(), tfidf);
        }

        if (did < 5) {
            TFIDF.clear();
            return;
        }
        //pick half and insert
        else {

            List<Map.Entry<String, Double>> list = new ArrayList<Map.Entry<String, Double>>(TFIDF.entrySet());
            Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
                @Override
                public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                    return o2.getValue().compareTo(o1.getValue());
                }
            });

            for (int i = 0; i < ceil(list.size() / (double) 2); i++) {
                keywordNum++;
                if (keywordTable.containsKey(list.get(i).getKey())) {
                    keywordTable.get(list.get(i).getKey()).add(did);
                } else {
                    keywordEntity kE = new keywordEntity(did);
                    keywordTable.put(list.get(i).getKey(), kE);

                    indexNum++;
                }
                if (DiskkeywordTable.containsKey(list.get(i).getKey())) {
                    DiskkeywordTable.get(list.get(i).getKey()).add(did);
                } else {
                    keywordEntity kE = new keywordEntity(did);
                    DiskkeywordTable.put(list.get(i).getKey(), kE);

                }
            }
            list.clear();

        }


    }

    public void analysisJson(String parent, Object objJson, int level) {
        //如果obj为json数组
        if (objJson instanceof JSONArray) { // iteration of []
            JSONArray objArray = (JSONArray) objJson;
            for (int i = 0; i < objArray.size(); i++) {
                analysisJson(parent, objArray.get(i), level);
            }
        } else if (objJson instanceof JSONObject) { //iteration of {}
            JSONObject obj = (JSONObject) objJson;
            //System.out.println(obj.keySet());
            for (Map.Entry<String, Object> entry : obj.entrySet()) {
                //System.out.println(entry.getKey() + ":" + entry.getValue());
                Object ojt = entry.getValue();

                //add parent/key to the hash table
                if (NameHash.containsKey(entry.getKey())) {
                    if (NameHash.get(entry.getKey()).find(level, parent) != null) {
                        //System.out.println("contain");
                    } else {
                        //System.out.println("add next");
                        NameHash.get(entry.getKey()).tail().append(level, parent);
                    }
                } else {
                    NameStructure na = new NameStructure(level, parent);

                    NameHash.put(entry.getKey(), na);
                }
                //System.out.println("add parent/key to the hash table level:"+level+parent);

                if (ojt instanceof JSONArray) {
                    //System.out.println("Array level:"+level);
                    JSONArray objArray = (JSONArray) ojt;
                    analysisJson(entry.getKey(), objArray, level + 1);
                } else if (ojt instanceof JSONObject) {
                    //System.out.println("Object level:"+level);
                    JSONObject objObject = (JSONObject) ojt;
                    analysisJson(entry.getKey(), objObject, level + 1);
                } else {
                    //System.out.println("leaf");
                    if (entry.getKey().equals("text") && level == 1) {
                        //System.out.println(ojt);
                        keywordExtraction((String) ojt);
                    } else {
                        newIndex++;
                        //add key to map/value to B-tree
                        int hashResult = hashFunc(ojt);
                        NameStructure tempNS = NameHash.get(entry.getKey()).find(level, parent);
                        treeID = (short) (treeID + tempNS.addIndex(treeID, did, hashResult, Forest, DiskForest, weight));

                    }

                }

            }
        }
    }

    public int liteQuery(ArrayList<String> queryList) {
        int found = 0;
        short id;
        for (int i = 0; i < queryList.size(); i += 2) {

            queryNum++;
            String str = queryList.get(i + 1);
            //System.out.println("query"+ queryList.get(i)+str);
            switch (queryList.get(i)) {
                case "1":
                    //$.text keyword table
                    if (keywordTable.containsKey(str)) {
                        if (keywordTable.get(str).dids.size() == DiskkeywordTable.get(str).dids.size())
                            found++;

                    } else {
                        keywordMiss++;
                    }
                    break;
                case "2":
                    //$.created_at
                    id = NameHash.get("created_at").find(1, "$").treeID;
                    if (Forest.get(id).containsKey(hashFunc(str))) {
                        //System.out.println("screen_name" + str +  Forest.get(id).get(hashFunc(str)));
                        ArrayList temp = (ArrayList) Forest.get(id).get(hashFunc(str));
                        ArrayList disktemp = (ArrayList) DiskForest.get(id).get(hashFunc(str));
                        if (disktemp.size() == temp.size())
                            found++;
                    } else {
                        System.out.println("created_at failed" + str);
                    }

                    break;
                case "3":
                    //$.user.followers_count
                    id = NameHash.get("followers_count").find(2, "user").treeID;
                    if (Forest.get(id).containsKey(hashFunc(Integer.parseInt(str)))) {
                        //System.out.println("screen_name" + str +  Forest.get(id).get(hashFunc(str)));
                        ArrayList temp = (ArrayList) Forest.get(id).get(hashFunc(Integer.parseInt(str)));
                        ArrayList disktemp = (ArrayList) DiskForest.get(id).get(hashFunc(Integer.parseInt(str)));
                        if (disktemp.size() == temp.size())
                            found++;
                    } else {
                        System.out.println("follower failed" + str);
                    }

                    break;
                case "4":
                    //$.user.friends_count
                    id = NameHash.get("friends_count").find(2, "user").treeID;
                    if (Forest.get(id).containsKey(hashFunc(Integer.parseInt(str)))) {
                        //System.out.println("screen_name" + str +  Forest.get(id).get(hashFunc(str)));
                        ArrayList temp = (ArrayList) Forest.get(id).get(hashFunc(Integer.parseInt(str)));
                        ArrayList disktemp = (ArrayList) DiskForest.get(id).get(hashFunc(Integer.parseInt(str)));
                        if (disktemp.size() == temp.size())
                            found++;
                    } else {
                        System.out.println("friends failed" + str);
                    }


                    break;
                case "5":
                    //$.user.favourites_count
                    id = NameHash.get("favourites_count").find(2, "user").treeID;
                    if (Forest.get(id).containsKey(hashFunc(Integer.parseInt(str)))) {
                        //System.out.println("favourites" + str +  Forest.get(id).get(hashFunc(str)));
                        ArrayList temp = (ArrayList) Forest.get(id).get(hashFunc(Integer.parseInt(str)));
                        ArrayList disktemp = (ArrayList) DiskForest.get(id).get(hashFunc(Integer.parseInt(str)));
                        if (disktemp.size() == temp.size())
                            found++;
                    } else {
                        System.out.println("favourites failed" + str);
                    }

                    break;
                /*case "7":
                    //$.user.name
                    id = NameHash.get("name").find(2,"user").treeID;
                    if(Forest.get(id).containsKey(hashFunc(str))){
                        //System.out.println("screen_name" + str +  Forest.get(id).get(hashFunc(str)));
                        found++;
                    }
                    else{
                        System.out.println("name failed" + str );
                    }

                    break;*/
                case "8":
                    //$.user.screen_name
                    id = NameHash.get("screen_name").find(2, "user").treeID;
                    if (Forest.get(id).containsKey(hashFunc(str))) {
                        //System.out.println("screen_name" + str +  Forest.get(id).get(hashFunc(str)));
                        ArrayList temp = (ArrayList) Forest.get(id).get(hashFunc(str));
                        ArrayList disktemp = (ArrayList) DiskForest.get(id).get(hashFunc(str));
                        if (disktemp.size() == temp.size())
                            found++;
                    } else {
                        System.out.println("screen name failed" + str);
                    }

                    break;
                case "9":
                    //$.user.listed_count
                    id = NameHash.get("listed_count").find(2, "user").treeID;
                    if (Forest.get(id).containsKey(hashFunc(Integer.parseInt(str)))) {
                        //System.out.println("screen_name" + str +  Forest.get(id).get(hashFunc(str)));
                        ArrayList temp = (ArrayList) Forest.get(id).get(hashFunc(Integer.parseInt(str)));
                        ArrayList disktemp = (ArrayList) DiskForest.get(id).get(hashFunc(Integer.parseInt(str)));
                        if (disktemp.size() == temp.size())
                            found++;
                    } else {
                        System.out.println("listed_count failed" + str);
                    }

                    break;
                case "10":
                    //$.retweeted_status.quote_count
                    id = NameHash.get("quote_count").find(2, "retweeted_status").treeID;
                    if (Forest.get(id).containsKey(hashFunc(Integer.parseInt(str)))) {
                        //System.out.println("screen_name" + str +  Forest.get(id).get(hashFunc(str)));
                        ArrayList temp = (ArrayList) Forest.get(id).get(hashFunc(Integer.parseInt(str)));
                        ArrayList disktemp = (ArrayList) DiskForest.get(id).get(hashFunc(Integer.parseInt(str)));
                        if (disktemp.size() == temp.size())
                            found++;
                    } else {
                        System.out.println("quote failed" + str);
                    }

                    break;
                default:
            }
        }
        return found;
    }

    public void weightPruning(int added) {
        //tempNum stands for the treesize of trees
        Map<Short, Integer> tempNum = new HashMap<Short, Integer>();
        Short tempw = 0;
        Random rand = new Random();
        for (Short i = 0; i < treeID; i++) {
            tempNum.put(i, treeSize(i));
        }

        while (added > 0) {
            tempw = weightMinkey(tempNum);
            TreeMap tempTree = Forest.get(tempw).tree;
            Integer[] keys = (Integer[]) tempTree.keySet().toArray(new Integer[0]);
            Integer randomKey;
            //System.out.println("keylength" + keys +keys.length);
            if (keys.length > 0) {
                randomKey = keys[rand.nextInt(keys.length)];
                ArrayList treetemp = (ArrayList) Forest.get(tempw).get(randomKey);
                added -= treetemp.size();
                //Forest.get(tempw).remove(randomKey);
                tempNum.put(tempw, treeSize(tempw));
                indexNum--;
            } else {

            }

        }
    }

    public void randomPruning(int added) {
        Random rand = new Random();
        int lucky = rand.nextInt(Forest.size() + 1);
        while (added > 0) {
            indexNum--;
            if (lucky == Forest.size()) {
                //prune from keyword
                Iterator it = keywordTable.keySet().iterator();
                String temp = it.next().toString();
                added -= keywordTable.get(temp).dids.size();
                keywordTable.remove(temp);
            } else {
                //prune from tree
                TreeMap tempTree = Forest.get((short) lucky).tree;
                while (tempTree.size() == 0) {
                    lucky = rand.nextInt(Forest.size());
                    tempTree = Forest.get((short) lucky).tree;
                }
                //System.out.println(tempTree);
                //System.out.println(added);
                Integer[] keys = (Integer[]) tempTree.keySet().toArray(new Integer[0]);
                Integer randomKey;
                randomKey = keys[rand.nextInt(keys.length)];
                ArrayList treetemp = (ArrayList) Forest.get((short) lucky).get(randomKey);
                added -= treetemp.size();
                //Forest.get((short) lucky).remove(randomKey);
            }
        }
    }

    private void pruningTestRead(HashMap<Integer, ArrayList> UPQuery) {
        //query check
        InputStream is = null;
        BufferedReader br = null;


        // read all the query
        try {
            is = getAssets().open("UserPreference.txt");
            Reader reader = new InputStreamReader(is);
            br = new BufferedReader(reader);

            String str;
            int i = 2000;

            int keywordsHit = 0;
            Object temp;
            Log.d("MyTag", "UP");
            ArrayList<String> tempq = new ArrayList<String>();
            UPQuery.put(2000, tempq);
            while ((str = br.readLine()) != null) {
                String[] words = str.split("\\s+", 2);
                if (words.length == 2) {
                    if (!words[0].equals("6") && !words[0].equals("7")) {
                        //Log.d("MyTag", str);
                        // query
                        UPQuery.get(i).add(words[0]);
                        UPQuery.get(i).add(words[1]);
                    }

                } else {
                    //Log.d("MyTag", str+"empty");
                    i++;
                    ArrayList<String> q = new ArrayList<String>();
                    UPQuery.put(i, q);

                }
                //i++;

            }
            Log.d("MyTag", "UPfinished");

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void pruningTestInsert(Cursor data, HashMap<Integer, ArrayList> UPQuery) {

        int tempqN = 0;
        int tempqS = 0;

        if (data.getCount() == 0) {
            Toast.makeText(this, "There are no contents in this list!", Toast.LENGTH_LONG).show();
        } else {
            while (data.moveToNext()) {
                //JSON arrives
                String halashao = data.getString(1);
                did = data.getInt(0);
                System.out.println(did + "daweitianlong");
                JSONArray json_str = null;
                JSONObject jsonObj = JSON.parseObject(halashao);


                if (pruning) {
                    //add to index
                    newIndex = 0;
                    analysisJson("$", jsonObj, 1);

                    if (did >= 2000) {
                        //pruning
                        if (did > 2000) {
                            //calculate reward and update the weight
                            reward = -10 * (queryNum - tempqN) + 11 * (querySuccess - tempqS);
                            //System.out.println("reward:"+reward);
                            qvalue_prev = getQ();
                            //System.out.println("qprev" + qvalue_prev);
                            if ((indexNum - 100000) > newIndex) {
                                qvalue_curr = getNextQ(newIndex);
                            } else {
                                qvalue_curr = getNextQ(indexNum - 100000);
                            }
                            //System.out.println("qcurr" + qvalue_curr);
                            difference = (reward + y * qvalue_curr) - qvalue_prev;
                            //System.out.println("diff:"+weight);
                            //System.out.println("difference:"+difference + "qcurr: " + qvalue_curr + "qprev: " + qvalue_prev);
                            updateWeight(difference);
                            //System.out.println("diff:"+weight);
                            //pruning according to the weight
                            if ((indexNum - 100000) > newIndex) {
                                weightPruning(newIndex);
                            } else {
                                weightPruning(indexNum - 100000);
                            }
                            //System.out.println("one more");
                        }
                        //randomPruning
                        /*
                        if((indexNum - 100000) >  newIndex){
                            randomPruning(newIndex);
                        }
                        else{
                            randomPruning(indexNum - 100000);
                        }*/
                        querySuccess += liteQuery(UPQuery.get(did));

                        if (did % 5 == 0) {
                            // replacement
                            Short k = weightMaxkey();
                            int rep = diskTreeSize(k) - treeSize(k);
                            //System.out.println("rep:" + rep + "k:" + k);
                            indexNum += rep;
                            fillTree(weightMaxkey());
                            weightPruning(rep);
                        }
                        if (did % 100 == 0) {
                            System.out.println("** querynum:" + (queryNum - tempqN));
                            System.out.println("** querysuccess:" + (querySuccess - tempqS));
                            tempqN = queryNum;
                            tempqS = querySuccess;

                        }
                    }

                    if (did >= 3000) {
                        System.out.println("indexnum:" + indexNum);
                        System.out.println("keywordnum:" + keywordTable.size());
                        System.out.println("keywordnumtotal:" + keywordNum);
                        System.out.println("wordnum:" + wordNum);
                        System.out.println("keywordMiss:" + keywordMiss);
                        return;
                        //break;
                    }

                } else {
                    //add to index
                    analysisJson("$", jsonObj, 1);

                    if (did >= 1000) {
                        System.out.println("indexnum:" + indexNum);
                        System.out.println("keywordnum:" + keywordTable.size());
                        System.out.println("keywordnumtotal:" + keywordNum);
                        System.out.println("wordnum:" + wordNum);
                        break;
                    }
                }


                //
                //theList.add(halashao);
                //JSONArray json_str = new JSONArray(theList);
                /*ListAdapter listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, theList);
                listView.setAdapter(listAdapter);*/

            }
        }
    }

    private void queryTest() {
        InputStream is = null;
        BufferedReader br = null;

        try {
            //general test

            is = getAssets().open("1000query.txt");
            Reader reader = new InputStreamReader(is);
            br = new BufferedReader(reader);

            String str;
            int i = 1;
                /*
                while ((str = br.readLine()) != null) {
                    Log.d("MyTag", str);
                    if(i > 260 && i < 361){
                        if (NameHash.get("favourites_count").find(2,"user")==null){
                            break;
                        }
                        else{
                            short id = NameHash.get("favourites_count").find(2,"user").treeID;
                            System.out.println(Forest.get(id).tailMap(Integer.parseInt(str)));
                        }
                    }
                    else if( i >= 860 && i < 960){
                        short id = NameHash.get("screen_name").find(2,"user").treeID;
                        System.out.println("screen_name" + str +  Forest.get(id).get(hashFunc(str)));
                    }
                    else{

                    }
                    i++;
                }

                */
            is = getAssets().open("keyword.txt");
            reader = new InputStreamReader(is);
            br = new BufferedReader(reader);

            i = 1;
            int keywordsHit = 0;
            Object temp;
            Log.d("MyTag", "1");
            while ((str = br.readLine()) != null) {
                //Log.d("MyTag", str);
                str = str.replaceAll("[^\\w]", "");
                if (keywordTable.containsKey(str)) {
                    keywordsHit++;
                    //System.out.println("keyword did list: "+keywordTable.get(str).dids);
                    temp = keywordTable.get(str).dids;
                }
                //i++;
            }
            Log.d("MyTag", "1finished");
            System.out.println("keyword hit: " + keywordsHit + "testTotal" + (i - 1));

                /*
                is = getAssets().open("follower.txt");
                reader = new InputStreamReader(is);
                br = new BufferedReader(reader);

                //i = 1;
                Log.d("MyTag", "4");
                while ((str = br.readLine()) != null) {
                    //Log.d("MyTag", str);
                    if(keywordTable.containsKey(str)){
                        short id = NameHash.get("followers_count").find(2,"user").treeID;
                        //System.out.println(Forest.get(id).tailMap(Integer.parseInt(str)));
                        Forest.get(id).tailMap(Integer.parseInt(str));
                    }
                    //i++;
                }
                Log.d("MyTag", "4finished");
                System.out.println("testTotal"+i);
                */
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.viewlistcontents_layout);

        ListView listView = (ListView) findViewById(R.id.listView);

        myDB = new DatabaseHelper(this);
        NameHash = new HashMap<String, NameStructure>();
        Forest = new HashMap<Short, treeRoot>();
        DiskForest = new HashMap<Short, treeRoot>();
        idf = new HashMap<String, Integer>();
        keywordTable = new HashMap<String, keywordEntity>();
        DiskkeywordTable = new HashMap<String, keywordEntity>();
        weight = new TreeMap<Short, Double>();

        //treeID from disk or 0
        treeID = 0;

        pruning = true;
        //populate an ArrayList<String> from the database and then view it
        //ArrayList<String> theList = new ArrayList<>();

        /*Cursor test = myDB.queryJSON1();
        System.out.println(test);*/

        HashMap<Integer, ArrayList> UPQuery = new HashMap<Integer, ArrayList>();
        pruningTestRead(UPQuery);


        Cursor data = myDB.getListContents();

        pruningTestInsert(data, UPQuery);


    }

}