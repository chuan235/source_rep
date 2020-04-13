package top.gmfcj.self;


public class MovieLaughObserver implements MovieObserver{

    @Override
    public void feel(SelfMovieEvent event) {
        if(event instanceof SelfLaughMovieEvent){
            System.out.println("...^_^...");
        }
    }
}
