
import lombok.Getter;

@Getter
public class Lock {

    private String lockKey;

    public Lock(String lockKey) {
        this.lockKey = lockKey;
    }
	
}
