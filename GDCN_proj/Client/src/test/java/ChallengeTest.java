import challenge.Challenge;
import challenge.Solution;
import org.testng.annotations.Test;

/**
 * Created by Leif on 2014-03-29.
 */
public class ChallengeTest {

    @Test
    public void testSolution(){
        Challenge challenge = Challenge.generate();
        Solution solution = Solution.solve(challenge);

        assert challenge.isSolution(solution);
    }
}