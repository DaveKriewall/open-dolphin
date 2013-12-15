package org.opendolphin.demo

import org.opendolphin.core.comm.Command
import org.opendolphin.core.comm.GetPresentationModelCommand
import org.opendolphin.core.server.DTO
import org.opendolphin.core.server.Slot
import org.opendolphin.core.server.action.DolphinServerAction
import org.opendolphin.core.server.comm.ActionRegistry
import org.opendolphin.core.server.comm.CommandHandler
import org.opendolphin.demo.data.Address
import org.opendolphin.demo.data.AddressGenerator

import static org.opendolphin.demo.LazyLoadingConstants.ATT.*
import static org.opendolphin.demo.LazyLoadingConstants.TYPE.*

public class LazyLoadingAction extends DolphinServerAction {

    List<Address> addressList

    public LazyLoadingAction(int numEntries) {
        addressList = new AddressGenerator().getAddressList(numEntries)
        // initial sorting
        Collections.sort(addressList, new Comparator<Address>() {
            @Override
            public int compare(Address address1, Address address2) {
                int result = address1.last.compareToIgnoreCase(address2.last)
                if (result == 0) {
                    result = address1.first.compareToIgnoreCase(address2.first)
                }
                if (result == 0) {
                    result = address1.city.compareToIgnoreCase(address2.city)
                }
                return result
            }
        })
    }

    @Override
    public void registerIn(ActionRegistry registry) {
        registry.register(GetPresentationModelCommand.class, new CommandHandler<GetPresentationModelCommand>() {
            public void handleCommand(GetPresentationModelCommand cmd, List<Command> response) {
                String pmId = cmd.pmId
                if (pmId == null) {
                    return
                }
                if (getServerDolphin().getAt(pmId) == null) {
                    initPresentationModel(pmId, response)
                }
            }
        })
    }

    private void initPresentationModel(String pmId, List<Command> response) {
        Address address = addressList.get(pmId.toInteger())
        presentationModel(pmId, LAZY, new DTO(
            new Slot( ID,           pmId),
            new Slot( FIRST,        address.first),
            new Slot( LAST,         address.last),
            new Slot( FIRST_LAST,   address.first + " " + address.last),
            new Slot( LAST_FIRST,   address.last + ", " + address.first),
            new Slot( CITY,         address.city),
            new Slot( PHONE,        address.phone),
        ))
    }
}
