package exceptions;

public class ProjectNotFoundException extends Exception{
    public ProjectNotFoundException(){
        super();
    }
    public ProjectNotFoundException(String msg){
        super(msg);
    }
}
