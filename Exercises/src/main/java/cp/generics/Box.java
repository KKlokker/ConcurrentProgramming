import java.util.ArrayList;

public class Box <T>{
    private final T content;
    
    public Box (T content) throws IllegalArgumentException{
        if(content == null)
            throw new IllegalArgumentException();
        this.content = content;
    }

    public T content() {
        return content;
    }

    public <O> O apply(BoxFunction<T,O> bfunc) {
        return bfunc.apply(content);
    }
    
    public static <T,O> ArrayList<O> applyToAll(ArrayList<Box<T>> list, BoxFunction<T,O> bfunc) {
        ArrayList<O> output = new ArrayList<O>();
        for(Box<T> b: list)
            output.add(b.apply(bfunc));
        return output;
    }
}
