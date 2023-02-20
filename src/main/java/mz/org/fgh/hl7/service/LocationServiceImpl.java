package mz.org.fgh.hl7.service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import mz.org.fgh.hl7.Location;

@Service
public class LocationServiceImpl implements LocationService {

    private static List<Location> locations;

    static {
        Location zambezia = new Location();
        zambezia.setId(31);
        zambezia.setName("Zambézia");
        zambezia.setUuid("770acffb-cfeb-46dc-92b0-9d6400f851b9");

        Location quelimane = new Location();
        quelimane.setId(435);
        quelimane.setName("Quelimane");
        quelimane.setUuid("af5ccc31-6ba9-47e4-b2f5-e87e1c1c5e07");

        Location cs17Setembro = new Location();
        cs17Setembro.setId(456);
        cs17Setembro.setName("CS 17 Setembro");
        cs17Setembro.setUuid("2beb99e3-580a-462e-9dae-c34ed1a86465");

        Location csChabeco = new Location();
        csChabeco.setId(457);
        csChabeco.setName("CS Chabeco");
        csChabeco.setUuid("660e84da-e72c-4080-b921-5131b77484cc");

        Location csCoalane = new Location();
        csCoalane.setId(436);
        csCoalane.setName("CS Coalane");
        csCoalane.setUuid("9aeb9a36-fca0-40f9-a172-0b6a770cb8c1");

        Location csIonge = new Location();
        csIonge.setId(488);
        csIonge.setName("CS Ionge");
        csIonge.setUuid("53ef4479-9515-485b-a7c4-3bdbe396278c");

        Location csZalala = new Location();
        csZalala.setId(484);
        csZalala.setName("CS Zalala");
        csZalala.setUuid("019744c1-f988-4d56-8cb9-596e8cf4e2dc");

        Location hpQuelimane = new Location();
        hpQuelimane.setId(437);
        hpQuelimane.setName("HP Quelimane");
        hpQuelimane.setUuid("e2b384fe-1d5f-11e0-b929-000c29ad1d07");

        quelimane.setChildLocations(Arrays.asList(cs17Setembro, csCoalane, csIonge, csZalala, hpQuelimane));

        Location altoMolocue = new Location();
        altoMolocue.setId(411);
        altoMolocue.setName("Alto Molocue");
        altoMolocue.setUuid("2c02bf63-dd58-4069-93f5-4c146a87e0ba");

        Location csNauela = new Location();
        csNauela.setId(412);
        csNauela.setName("CS Nauela");
        csNauela.setUuid("e2b33422-1d5f-11e0-b929-000c29ad1d07");

        Location csMoiua = new Location();
        csMoiua.setId(414);
        csMoiua.setName("CS Moiua");
        csMoiua.setUuid("a44943c6-94fe-4a34-9ac4-f0df4759b859");

        Location csMutala = new Location();
        csMutala.setId(415);
        csMutala.setName("CS Mutala");
        csMutala.setUuid("c210dcaf-9ef0-4685-a019-42bdb129dd66");

        Location csCaiaia = new Location();
        csCaiaia.setId(416);
        csCaiaia.setName("CS Caiaia");
        csCaiaia.setUuid("4e5bc6e3-574e-4fa6-addf-ff0365b28f23");

        altoMolocue.setChildLocations(Arrays.asList(csNauela, csMoiua, csMutala, csCaiaia));

        zambezia.setChildLocations(Arrays.asList(quelimane, altoMolocue));

        locations = Arrays.asList(zambezia, quelimane, cs17Setembro, csChabeco, csCoalane,
                csIonge, csZalala, hpQuelimane, altoMolocue, csNauela, csMoiua, csMutala, csCaiaia);
    }

    public Location findByUuid(String uuid) {
        Optional<Location> location = locations.stream()
                .filter(l -> l.getUuid().equals(uuid)).findFirst();
        if (location.isPresent()) {
            return location.get();
        }
        throw new RuntimeException("Not found");
    }

    public Location findById(long id) {
        Optional<Location> location = locations.stream()
                .filter(l -> l.getId() == id).findFirst();
        if (location.isPresent()) {
            return location.get();
        }
        throw new RuntimeException("Not found");
    }
}
