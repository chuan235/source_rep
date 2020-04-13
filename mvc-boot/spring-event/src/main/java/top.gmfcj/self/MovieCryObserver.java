package top.gmfcj.self;


public class MovieCryObserver implements MovieObserver{

    @Override
    public void feel(SelfMovieEvent event) {
        if(event instanceof SelfCryMovieEvent){
            System.out.println("...—_—...");
        }
    }
}
