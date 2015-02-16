/*
 * <summary></summary>
 * <author>He Han</author>
 * <email>hankcs.cn@gmail.com</email>
 * <create-date>2015/2/16 21:50</create-date>
 *
 * <copyright file="State.java" company="上海林原信息科技有限公司">
 * Copyright (c) 2003-2014, 上海林原信息科技有限公司. All Right Reserved, http://www.linrunsoft.com/
 * This source is subject to the LinrunSpace License. Please contact 上海林原信息科技有限公司 to get more information.
 * </copyright>
 */
package com.hankcs.hanlp.algoritm.automata;

import com.hankcs.hanlp.corpus.io.ByteArray;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;

/**
 * 二阶自动机（基于类双数组Trie树原理）
 * @author hankcs
 */
public class BiAutomata<V>
{
    /**
     * 双数组值check
     */
    protected int check[];
    /**
     * 双数组之base
     */
    protected int base[];
    /**
     * 保存value
     */
    protected V[] v;
    /**
     * base 和 check 的大小
     */
    protected int size;

    public void save(DataOutputStream out) throws Exception
    {
        out.writeInt(size);
        for (int i = 0; i < size; i++)
        {
            out.writeInt(base[i]);
            out.writeInt(check[i]);
        }
    }

    public void save(ObjectOutputStream out) throws IOException
    {
        out.writeObject(base);
        out.writeObject(check);
    }

    public void load(ObjectInputStream in, V[] value) throws IOException, ClassNotFoundException
    {
        base = (int[]) in.readObject();
        check = (int[]) in.readObject();
        v = value;
    }

    public boolean load(ByteArray byteArray, V[] value)
    {
        if (byteArray == null) return false;
        size = byteArray.nextInt();
        base = new int[size + 65535];   // 多留一些，防止越界
        check = new int[size + 65535];
        int length;
        for (int i = 0; i < size; ++i)
        {
            base[i] = byteArray.nextInt();
            check[i] = byteArray.nextInt();
        }
        return true;
    }

    /**
     * 获取值
     * @param key 键
     * @return
     */
    public V get(int... key)
    {
        int index = exactMatchSearch(key);
        if (index >= 0)
        {
            return v[index];
        }

        return null;
    }

    /**
     * 从值数组中提取下标为index的值<br>
     *     注意为了效率，此处不进行参数校验
     * @param index 下标
     * @return 值
     */
    public V get(int index)
    {
        return v[index];
    }

    /**
     * 转移状态
     *
     * @param current
     * @param c
     * @return
     */
    protected int transition(int current, char c)
    {
        int b = current;
        int p;

        p = b + c + 1;
        if (b == check[p])
            b = base[p];
        else
            return -1;

        p = b;
        return p;
    }

    /**
     * c转移，如果是根节点则返回自己
     *
     * @param nodePos
     * @param c
     * @return
     */
    protected int transitionWithRoot(int nodePos, char c)
    {
        int b = base[nodePos];
        int p;

        p = b + c + 1;
        if (b != check[p])
        {
            if (nodePos == 0) return 0;
            return -1;
        }

        return p;
    }


    /**
     * 由一个排序好的map创建
     */
    public void build(TreeMap<Integer, TreeMap<Integer, V>> map)
    {
        new Builder().build(map);
    }

    /**
     * 获取直接相连的子节点
     *
     * @param parent   父节点
     * @param siblings （子）兄弟节点
     * @return 兄弟节点个数
     */
    private int fetch(State parent, List<Map.Entry<Integer, State>> siblings)
    {
        if (parent.isAcceptable())
        {
            State fakeNode = new State(-(parent.getDepth() + 1));  // 此节点是parent的子节点，同时具备parent的输出
            fakeNode.addEmit(parent.emit());
            siblings.add(new AbstractMap.SimpleEntry<Integer, State>(0, fakeNode));
        }
        for (Map.Entry<Integer, State> entry : parent.getSuccess().entrySet())
        {
            siblings.add(new AbstractMap.SimpleEntry<Integer, State>(entry.getKey() + 1, entry.getValue()));
        }
        return siblings.size();
    }

    /**
     * 精确匹配
     *
     * @param key 键
     * @return 值的下标
     */
    public int exactMatchSearch(int... key)
    {
        return exactMatchSearch(key, 0, 0, 0);
    }

    /**
     * 精确匹配
     *
     * @param key
     * @param pos
     * @param len
     * @param nodePos
     * @return
     */
    private int exactMatchSearch(int[] key, int pos, int len, int nodePos)
    {
        if (len <= 0)
            len = key.length;
        if (nodePos <= 0)
            nodePos = 0;

        int result = -1;

        int[] keyChars = key;

        int b = base[nodePos];
        int p;

        for (int i = pos; i < len; i++)
        {
            p = b + (int) (keyChars[i]) + 1;
            if (b == check[p])
                b = base[p];
            else
                return result;
        }

        p = b;
        int n = base[p];
        if (b == check[p] && n < 0)
        {
            result = -n - 1;
        }
        return result;
    }

    /**
     * 精确查询
     *
     * @param keyChars 键的char数组
     * @param pos      char数组的起始位置
     * @param len      键的长度
     * @param nodePos  开始查找的位置（本参数允许从非根节点查询）
     * @return 查到的节点代表的value ID，负数表示不存在
     */
    private int exactMatchSearch(char[] keyChars, int pos, int len, int nodePos)
    {
        int result = -1;

        int b = base[nodePos];
        int p;

        for (int i = pos; i < len; i++)
        {
            p = b + (int) (keyChars[i]) + 1;
            if (b == check[p])
                b = base[p];
            else
                return result;
        }

        p = b;
        int n = base[p];
        if (b == check[p] && n < 0)
        {
            result = -n - 1;
        }
        return result;
    }

//    public void dfs(IWalker walker)
//    {
//        dfs(rootState, "", walker);
//    }
//
//    private void dfs(State currentState, String path, IWalker walker)
//    {
//        walker.meet(path, currentState);
//        for (Character transition : currentState.getTransitions())
//        {
//            State targetState = currentState.nextState(transition);
//            dfs(targetState, path + transition, walker);
//        }
//    }
//
//
//    public static interface IWalker
//    {
//        /**
//         * 遇到了一个节点
//         *
//         * @param path
//         * @param state
//         */
//        void meet(String path, State state);
//    }
//

//    public void debug()
//    {
//        System.out.println("base:");
//        for (int i = 0; i < base.length; i++)
//        {
//            if (base[i] < 0)
//            {
//                System.out.println(i + " : " + -base[i]);
//            }
//        }
//
//        System.out.println("output:");
//        for (int i = 0; i < output.length; i++)
//        {
//            if (output[i] != null)
//            {
//                System.out.println(i + " : " + Arrays.toString(output[i]));
//            }
//        }
//
//        System.out.println("fail:");
//        for (int i = 0; i < fail.length; i++)
//        {
//            if (fail[i] != 0)
//            {
//                System.out.println(i + " : " + fail[i]);
//            }
//        }
//
//        System.out.println(this);
//    }

//    @Override
//    public String toString()
//    {
//        String infoIndex = "i    = ";
//        String infoChar = "char = ";
//        String infoBase = "base = ";
//        String infoCheck = "check= ";
//        for (int i = 0; i < Math.min(base.length, 200); ++i)
//        {
//            if (base[i] != 0 || check[i] != 0)
//            {
//                infoChar += "    " + (i == check[i] ? " ×" : (char) (i - check[i] - 1));
//                infoIndex += " " + String.format("%5d", i);
//                infoBase += " " + String.format("%5d", base[i]);
//                infoCheck += " " + String.format("%5d", check[i]);
//            }
//        }
//        return "DoubleArrayTrie：" +
//                "\n" + infoChar +
//                "\n" + infoIndex +
//                "\n" + infoBase +
//                "\n" + infoCheck + "\n" +
////                "check=" + Arrays.toString(check) +
////                ", base=" + Arrays.toString(base) +
////                ", used=" + Arrays.toString(used) +
//                "size=" + size +
//                ", allocSize=" + allocSize +
//                ", keySize=" + keySize +
////                ", length=" + Arrays.toString(length) +
////                ", value=" + Arrays.toString(value) +
//                ", progress=" + progress +
//                ", nextCheckPos=" + nextCheckPos
//                ;
//    }

    /**
     * 一个顺序输出变量名与变量值的调试类
     */
    private static class DebugArray
    {
        Map<String, String> nameValueMap = new LinkedHashMap<String, String>();

        public void add(String name, int value)
        {
            String valueInMap = nameValueMap.get(name);
            if (valueInMap == null)
            {
                valueInMap = "";
            }

            valueInMap += " " + String.format("%5d", value);

            nameValueMap.put(name, valueInMap);
        }

        @Override
        public String toString()
        {
            String text = "";
            for (Map.Entry<String, String> entry : nameValueMap.entrySet())
            {
                String name = entry.getKey();
                String value = entry.getValue();
                text += String.format("%-5s", name) + "= " + value + '\n';
            }

            return text;
        }

        public void println()
        {
            System.out.print(this);
        }
    }

    /**
     * 大小，即包含多少个模式串
     * @return
     */
    public int size()
    {
        return v.length;
    }

    /**
     * 构建工具
     */
    private class Builder
    {
        /**
         * 根节点，仅仅用于构建过程
         */
        private State rootState = new State();
        /**
         * 是否占用，仅仅用于构建
         */
        private boolean used[];
        /**
         * 已分配在内存中的大小
         */
        private int allocSize;
        /**
         * 一个控制增长速度的变量
         */
        private int progress;
        /**
         * 下一个插入的位置将从此开始搜索
         */
        private int nextCheckPos;
        /**
         * 键值对的大小
         */
        private int keySize;

        /**
         * 由一个排序好的map创建
         */
        @SuppressWarnings("unchecked")
        public void build(TreeMap<Integer, TreeMap<Integer, V>> map)
        {
            // 把值保存下来
            v = (V[]) map.values().toArray();
            Set<Integer> keySet = map.keySet();
            // 构建二分trie树
            buildTrieTree(map);
            // 在二分trie树的基础上构建双数组trie树
            buildDoubleArrayTrie(keySet);
            used = null;
            rootState = null;
            loseWeight();
        }

        /**
         * 构建trie树，当然，这里深度最大为2
         *
         * @param map
         */
        private void buildTrieTree(TreeMap<Integer, TreeMap<Integer, V>> map)
        {
            int i = 0;
            for (Map.Entry<Integer, TreeMap<Integer, V>> entry : map.entrySet())
            {
                State first = this.rootState.addState(entry.getKey());
                for (Map.Entry<Integer, V> to : entry.getValue().entrySet())
                {
                    State second = first.addState(to.getKey());
                    second.addEmit(i++);
                }
            }
        }

        private void buildDoubleArrayTrie(Set<Integer> keySet)
        {
            progress = 0;
            keySize = keySet.size();
            resize(65536 * 32); // 32个双字节

            base[0] = 1;
            nextCheckPos = 0;

            State root_node = this.rootState;

            List<Map.Entry<Integer, State>> siblings = new ArrayList<Map.Entry<Integer, State>>(root_node.getSuccess().entrySet().size());
            fetch(root_node, siblings);
            insert(siblings);
        }

        /**
         * 拓展数组
         *
         * @param newSize
         * @return
         */
        private int resize(int newSize)
        {
            int[] base2 = new int[newSize];
            int[] check2 = new int[newSize];
            boolean used2[] = new boolean[newSize];
            if (allocSize > 0)
            {
                System.arraycopy(base, 0, base2, 0, allocSize);
                System.arraycopy(check, 0, check2, 0, allocSize);
                System.arraycopy(used, 0, used2, 0, allocSize);
            }

            base = base2;
            check = check2;
            used = used2;

            return allocSize = newSize;
        }

        /**
         * 插入节点
         *
         * @param siblings 等待插入的兄弟节点
         * @return 插入位置
         */
        private int insert(List<Map.Entry<Integer, State>> siblings)
        {
            int begin = 0;
            int pos = Math.max(siblings.get(0).getKey() + 1, nextCheckPos) - 1;
            int nonzero_num = 0;
            int first = 0;

            if (allocSize <= pos)
                resize(pos + 1);

            outer:
            // 此循环体的目标是找出满足base[begin + a1...an]  == 0的n个空闲空间,a1...an是siblings中的n个节点
            while (true)
            {
                pos++;

                if (allocSize <= pos)
                    resize(pos + 1);

                if (check[pos] != 0)
                {
                    nonzero_num++;
                    continue;
                }
                else if (first == 0)
                {
                    nextCheckPos = pos;
                    first = 1;
                }

                begin = pos - siblings.get(0).getKey(); // 当前位置离第一个兄弟节点的距离
                if (allocSize <= (begin + siblings.get(siblings.size() - 1).getKey()))
                {
                    // progress can be zero // 防止progress产生除零错误
                    double l = (1.05 > 1.0 * keySize / (progress + 1)) ? 1.05 : 1.0 * keySize / (progress + 1);
                    resize((int) (allocSize * l));
                }

                if (used[begin])
                    continue;

                for (int i = 1; i < siblings.size(); i++)
                    if (check[begin + siblings.get(i).getKey()] != 0)
                        continue outer;

                break;
            }

            // -- Simple heuristics --
            // if the percentage of non-empty contents in check between the
            // index
            // 'next_check_pos' and 'check' is greater than some constant value
            // (e.g. 0.9),
            // new 'next_check_pos' index is written by 'check'.
            if (1.0 * nonzero_num / (pos - nextCheckPos + 1) >= 0.95)
                nextCheckPos = pos; // 从位置 next_check_pos 开始到 pos 间，如果已占用的空间在95%以上，下次插入节点时，直接从 pos 位置处开始查找
            used[begin] = true;

            size = (size > begin + siblings.get(siblings.size() - 1).getKey() + 1) ? size : begin + siblings.get(siblings.size() - 1).getKey() + 1;

            for (Map.Entry<Integer, State> sibling : siblings)
            {
                check[begin + sibling.getKey()] = begin;
            }

            for (Map.Entry<Integer, State> sibling : siblings)
            {
                List<Map.Entry<Integer, State>> new_siblings = new ArrayList<Map.Entry<Integer, State>>(sibling.getValue().getSuccess().entrySet().size() + 1);

                if (fetch(sibling.getValue(), new_siblings) == 0)  // 一个词的终止且不为其他词的前缀，其实就是叶子节点
                {
                    base[begin + sibling.getKey()] = (-sibling.getValue().emit() - 1);
                    progress++;
                }
                else
                {
                    int h = insert(new_siblings);   // dfs
                    base[begin + sibling.getKey()] = h;
                }
                sibling.getValue().setIndex(begin + sibling.getKey());
            }
            return begin;
        }

        private void loseWeight()
        {
            int nbase[] = new int[size + 65535];
            System.arraycopy(base, 0, nbase, 0, size);
            base = nbase;

            int ncheck[] = new int[size + 65535];
            System.arraycopy(check, 0, ncheck, 0, size);
            check = ncheck;
        }
    }
}