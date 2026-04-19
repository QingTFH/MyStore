package tools;

import com.oocourse.elevator3.PersonRequest;
import main.Config;

public class RequestFactory {

    public static PersonRequest newPersonRequest(int curFloor, PersonRequest request) {
        return new PersonRequest(
                Config.changeFloorToString(curFloor),
                request.getToFloor(),
                request.getPersonId(),
                request.getWeight()
        );
    }

}
