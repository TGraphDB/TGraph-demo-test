package org.act.tgraph.demo;

import org.junit.internal.TextListener;
import org.junit.runner.*;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

/**
 * Created by song on 16-2-28.
 */
public class Main {

    public static void main(String... args) throws ClassNotFoundException, InitializationError, NoTestsRemainException {
        String[] classAndMethod = args[0].split("/");
        Runner runner = new BlockJUnit4ClassRunner(Class.forName(classAndMethod[0]));
        new NameFilter(classAndMethod[1]).apply(runner);

        JUnitCore junit = new JUnitCore();
        junit.addListener(new TextListener(System.out));

        Result result = junit.run(runner);
//        result = JUnitCore.runClasses(Class.forName(System.getProperty("t")));
        for (Failure fail : result.getFailures()) {
            System.out.println(fail.toString());
        }
        if (result.wasSuccessful()) {
            System.out.println("\nTest finished successfully :)");
            System.exit(0);
        }else {
            System.out.println("\nTest failed :P");
            System.exit(183);
        }
    }

    private static class NameFilter extends Filter{
        private String methodName;

        public NameFilter(String methodName){
            this.methodName = methodName;
        }

        @Override
        public boolean shouldRun(Description description) {
//            System.out.println(description.getMethodName());
            if(methodName.equals(description.getMethodName())){

                return true;
            }else{
                return false;
            }
        }

        @Override
        public String describe() {
            return "one-method-run-filter";
        }
    }

}
