package test;

/**
 * Created by cloud on 2019/03/19.
 */
public class Test {

    public static void main(String[] args) {
        Bird swallow = Bird.builder().age(1).name("swallow").build();

        Bird kiwi = Bird.builder().age(2).name("kiwi").build();

        swallow.eat();

        swallow.sing("kiki");

        swallow.flyHign(1);

    }

}
