package top.gmfcj.lettcode;

import com.google.common.collect.ForwardingSortedSet;
import com.sun.jmx.remote.internal.ArrayQueue;

import java.util.*;

/**
 * 面试 top48：https://yq.aliyun.com/articles/646175
 */
public class ArrayCode {


    public static void main(String[] args) {
        ArrayCode code = new ArrayCode();
//        code.findMissNumber(new int[]{1, 2, 3, 4, 3, 4, 5}, 10);
//        code.findDupNumber(new int[]{1, 2, 3, 4, 3, 4, 5});
//        code.finMaxAndMinNumber(new int[]{8, 1, 2, 3, 4, 3, 4, 5});
        int[] ints = {8, 1, 2, 3, 4, 3, 4, 5};
//        code.removeDupNumber(ints);
//        ArrayCode.findPairsNumber(0);
//        for(int i=0;i<10;i++){
//            System.out.println(new Random().nextInt(ints.length));
//        }
        int[] quickArr = {4, 7, 6, 5, 3, 2, 8, 1, 2, 5, 4};

//        quickSort(quickArr, 0, quickArr.length - 1);
//        quickSort2(quickArr, 0, quickArr.length - 1);
        quickSortStack(quickArr, 0, quickArr.length - 1);

    }

    /**
     * 在一个元素为 1 到 100 的整数数组中，如何搜索缺失元素
     *
     * @param nums
     * @return
     */
    public void findMissNumber(int[] nums, int range) {
        System.out.printf("找出%s中缺失的元素,查询的范围是0-%d \n", Arrays.toString(nums), range);
        int bitSet = 0;
        // 如果哪一位存在值，那么这个int的多少为就会是1
        for (int num : nums) {
            // 1 << 0   0001
            // 1 << 1   0010
            // 1 << 2   0100
            // 1 << 3   1000
            // 1 << 4  10000
            bitSet |= 1 << num;
        }
        for (int i = 0; i < range; i++) {
            // 如果对应的位上存在值，这里就是1
            if ((bitSet & 1 << i) == 0) {
                System.out.println("缺失的数：" + i);
            }
        }
    }

    /**
     * 给定一个数组，如何搜索重复元素？
     *
     * @param nums
     */
    public static void findDupNumber(int[] nums) {
        System.out.printf("找出%s中的重复的元素\n", Arrays.toString(nums));
        int bitSet = 0;
        for (int num : nums) {
            if ((bitSet & 1 << num) != 0) {
                System.out.println("存在重复值：" + num);
            }
            bitSet |= 1 << num;
        }
    }

    /**
     * 给定一个乱序数组，如何搜索最大和最小元素
     * 先排序 -> 出来了
     * 不排序 -> 依次比较
     *
     * @param nums
     */
    public static void finMaxAndMinNumber(int[] nums) {
        System.out.printf("查询数组中 %s 的最大值和最小值\n", Arrays.toString(nums));
        int min = Integer.MIN_VALUE;
        int max = Integer.MAX_VALUE;
        for (int num : nums) {
            if (num > max) {
                max = num;
            }
            if (num < min) {
                min = num;
            }
        }
        System.out.printf("查询完成，最小值是%d,最大值是%d \n", min, max);
    }

    /**
     * 给定一个数值，如何搜索整数数组中加和为该数值的成对元素
     * 回溯
     * 1、数组是否排序，未排序的话首先进排序
     * 2、回溯 递归
     * [1,2,3,4,7,9,12,15]   28
     */
    private static int total = 28;
    private static int[] nums = {1, 2, 3, 4, 7, 9, 12, 15};
    private static int tempSum = 0;
    private static Stack<Integer> stack = new Stack<>();

    // int[] nums, int tempSum, int total,queue
    public static void findPairsNumber(int index) {
        if (tempSum > total) return;
        if (tempSum == total) {
            // 打印结果并返回
            System.out.print("和为" + tempSum + "的组合：[");
            for (Integer e : stack) {
                System.out.print(e + " ");
            }
            System.out.print("]\n");
            return;
        }
        for (int i = index; i < nums.length; i++) {
            int num = nums[i];
            stack.push(num);
            tempSum += num;
            findPairsNumber(i + 1);
            stack.pop();
            tempSum -= num;
        }
    }

    /**
     * 给定一个数组，如何用 Java 删除重复元素？
     * 如何在不使用库的情况下移除数组中的重复元素？
     *
     * @param nums
     */
    public static void removeDupNumber(int[] nums) {
        System.out.printf("删除%s中的重复的元素\n", Arrays.toString(nums));
        int[] noDumArr = new int[nums.length];
        int bitSet = 0;
        int index = 0;
        for (int i = 0; i < nums.length; i++) {
            int num = nums[i];
            if ((bitSet & 1 << num) != 0) {
                System.out.println("存在重复值：" + num);
                continue;
            }
            bitSet |= 1 << num;
            noDumArr[index++] = num;
        }
        System.out.println("删除后的数据:" + Arrays.toString(noDumArr));
    }

    /**
     * 如何使用 Java 反转一个数组
     */
    public static void reversArray(int[] nums) {
        System.out.printf("对数组%s进行反转\n", Arrays.toString(nums));
        int[] reversArr = new int[nums.length];
        for (int i = 0; i < nums.length; i++) {
            reversArr[i] = nums[nums.length - 1 - i];
        }
        System.out.printf("反转后的数组" + Arrays.toString(nums));
    }

    /**
     * 快速排序算法对整数数组进行排序
     *
     * @param nums        数组
     * @param left        leftIndex
     * @param right       rightIndex
     * @param randomIndex 基准值的索引
     */
    public static boolean needRandom = true;

    public static void quickSort(int[] nums, int left, int right) {
        if (left >= right) {
            return;
        }

        System.out.printf("对数组%s进行快速排序，升序\n", Arrays.toString(nums));
        // 将每次left位和随机一位交换位置，这个这个基准值就是随机的
        if (needRandom && left == 0) {
            swap(nums, 0, new Random().nextInt(nums.length));
            System.out.printf("对数组%s进行第一次随机交换\n", Arrays.toString(nums));
            needRandom = false;
        }
        int pivotIndex = partition(nums, left, right);
        quickSort(nums, left, pivotIndex - 1);
        quickSort(nums, pivotIndex + 1, right);
    }


    public static int partition(int[] nums, int left, int right) {
        System.out.printf("排序前的数组：%s,排序范围 %d - %d \n", Arrays.toString(nums), left, right);
        int pivotIndex = left;
        int pivot = nums[pivotIndex];
        System.out.println("基准值是：" + pivot);
        // 指针交换法
        while (left != right) {
            // 最开始比较右指针  可以升序排列  这里就可以确定left==right的时候，可以准确的让基准值和left交换
            while (left < right && nums[right] > pivot) {
                // 右指针左移
                right--;
            }
            while (left < right && nums[left] <= pivot) {
                // 左指针右移
                left++;
            }
            // nums[left] > pivot nums[right] < pivot
            if (left < right) {
                // 交换左右指针的位置
                swap(nums, left, right);
            }
        }
        // 相等的时候
        swap(nums, left, pivotIndex);
        System.out.println("排序后的数组：" + Arrays.toString(nums));
        return left;
    }

    /*
     * 交换数组 arr 中下标为 i 和下标为 j 位置的元素
     */
    public static void swap(int[] arr, int i, int j) {
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }

    /**
     * 挖坑法
     *
     * @param nums
     * @param left
     * @param right
     */
    public static void quickSort2(int[] nums, int left, int right) {
        if (left >= right) {
            return;
        }
        int pivot = partition2(nums, left, right);
        quickSort2(nums, left, pivot - 1);
        quickSort2(nums, pivot + 1, right);
    }

    private static int partition2(int[] nums, int left, int right) {
        int pivotIndex = left;
        int pivot = nums[pivotIndex];
        System.out.println("对数组进行排序start：" + Arrays.toString(nums));
        System.out.println("基准值是：" + pivot);
        while (left < right) {
            // 右指针开始
            while (left < right) {
                if (nums[right] < pivot) {
                    // 交换右指针和坑的位置
                    nums[pivotIndex] = nums[right];
                    pivotIndex = right;
                    left++;
                    break;
                }
                right--;
            }
            // 左指针开始
            while (left < right) {
                if (nums[left] > pivot) {
                    nums[pivotIndex] = nums[left];
                    pivotIndex = left;
                    right--;
                    break;
                }
                left++;
            }
        }
        nums[pivotIndex] = pivot;
        System.out.println("return pivotIndex=" + pivotIndex);
        System.out.println("对数组进行排序end：" + Arrays.toString(nums));
        return left;
    }

    /**
     * 使用栈数据结构替换递归
     * @param nums
     * @param left
     * @param right
     */
    public static void quickSortStack(int[] nums, int left, int right) {
        // {left:0,right:length-1}
        Stack<Map<String, Integer>> sortStack = new Stack<>();
        // 将排序的逻辑入栈
        sortStack.push(buildMethodMap(left, right));
        while (!sortStack.isEmpty()) {
            Map<String, Integer> map = sortStack.pop();
            Integer mapLeft = map.get("left");
            Integer mapRight = map.get("right");
            // 找出基准值
            int pivotIndex = partition(nums, mapLeft, mapRight);
            // 入栈
            if (mapLeft < pivotIndex - 1) {
                sortStack.push(buildMethodMap(mapLeft, pivotIndex - 1));
            }
            if (mapRight > pivotIndex + 1) {
                sortStack.push(buildMethodMap(pivotIndex + 1, mapRight));
            }
        }

    }

    public static Map<String, Integer> buildMethodMap(Integer left, Integer right) {
        Map<String, Integer> map = new HashMap<>();
        map.put("left", left);
        map.put("right", right);
        return map;
    }


}
