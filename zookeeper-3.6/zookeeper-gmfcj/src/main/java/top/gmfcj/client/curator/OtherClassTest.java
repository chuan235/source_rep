package top.gmfcj.client.curator;


import com.google.common.collect.Lists;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public class OtherClassTest {

    public static void main(String[] args) {
        BigDecimal flagWeight = new BigDecimal(-11);
        double allowNum = 0.1;
        boolean zzallowNum = flagWeight.doubleValue() < -allowNum;

        if (zzallowNum) {
            // >0.1  <-0.1  <-0.1
            System.out.println("<-0.1");
        }

    }

}
