package com.minecolonies.coremod.client.gui;

import com.ldtteam.blockout.Pane;
import com.ldtteam.blockout.controls.Button;
import com.ldtteam.blockout.controls.ButtonImage;
import com.ldtteam.blockout.controls.ItemIcon;
import com.ldtteam.blockout.controls.Text;
import com.ldtteam.blockout.views.ScrollingList;
import com.ldtteam.structurize.util.LanguageHandler;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.util.Tuple;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.coremod.Network;
import com.minecolonies.coremod.colony.buildings.workerbuildings.BuildingBaker;
import com.minecolonies.coremod.network.messages.server.colony.building.RemoveMinimumStockFromBuildingMessage;
import com.minecolonies.coremod.util.FurnaceRecipes;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

import static com.minecolonies.api.util.constant.TranslationConstants.COM_MINECOLONIES_COREMOD_GUI_BAKER;
import static com.minecolonies.api.util.constant.WindowConstants.*;

/**
 * Baker window class. Specifies the extras the bakery has for its list.
 */
public class WindowHutBakerModule extends AbstractWindowWorkerModuleBuilding<BuildingBaker.View>
{
    /**
     * The resource string.
     */
    private static final String RESOURCE_STRING = ":gui/windowhutbaker.xml";

    /**
     * Limit reached label.
     */
    private static final String LABEL_LIMIT_REACHED = "com.minecolonies.coremod.gui.warehouse.limitreached";

    /**
     * Resource scrolling list.
     */
    private final ScrollingList resourceList;

    /**
     * Constructor for the window of the bakery.
     *
     * @param building {@link BuildingBaker.View}.
     */
    public WindowHutBakerModule(final BuildingBaker.View building)
    {
        super(building, Constants.MOD_ID + RESOURCE_STRING);

        resourceList = this.window.findPaneOfTypeByID("resourcesstock", ScrollingList.class);

        registerButton(STOCK_ADD, this::addStock);
        if (building.hasReachedLimit())
        {
            final ButtonImage button = findPaneOfTypeByID(STOCK_ADD, ButtonImage.class);
            button.setText(LanguageHandler.format(LABEL_LIMIT_REACHED));
            button.setImage(new ResourceLocation(Constants.MOD_ID, "textures/gui/builderhut/builder_button_medium_dark.png"));
        }

        registerButton(STOCK_REMOVE, this::removeStock);
    }

    /**
     * Returns the name of a building.
     *
     * @return Name of a building.
     */
    @NotNull
    @Override
    public String getBuildingName()
    {
        return COM_MINECOLONIES_COREMOD_GUI_BAKER;
    }

    /**
     * Remove the stock.
     *
     * @param button the button.
     */
    private void removeStock(final Button button)
    {
        final int row = resourceList.getListElementIndexByPane(button);
        final Tuple<ItemStorage, Integer> tuple = building.getStock().get(row);
        building.getStock().remove(row);
        Network.getNetwork().sendToServer(new RemoveMinimumStockFromBuildingMessage(building, tuple.getA().getItemStack()));
        updateStockList();
    }

    /**
     * Add the stock.
     */
    private void addStock()
    {
        if (!building.hasReachedLimit())
        {
            new WindowSelectRes(this, building,
              itemStack -> ItemStackUtils.CAN_EAT.test(itemStack) || ItemStackUtils.CAN_EAT.test(FurnaceRecipes.getInstance().getSmeltingResult(itemStack)) || building.getRecipes().stream().anyMatch(r -> ItemStackUtils.compareItemStacksIgnoreStackSize(itemStack, r.getPrimaryOutput()))).open();
        }
    }

    @Override
    public void onOpened()
    {
        super.onOpened();
        updateStockList();
    }

    /**
     * Updates the resource list in the GUI with the info we need.
     */
    private void updateStockList()
    {
        resourceList.enable();
        resourceList.show();
        final List<Tuple<ItemStorage, Integer>> tempRes = new ArrayList<>(building.getStock());

        //Creates a dataProvider for the unemployed resourceList.
        resourceList.setDataProvider(new ScrollingList.DataProvider()
        {
            /**
             * The number of rows of the list.
             * @return the number.
             */
            @Override
            public int getElementCount()
            {
                return tempRes.size();
            }

            /**
             * Inserts the elements into each row.
             * @param index the index of the row/list element.
             * @param rowPane the parent Pane for the row, containing the elements to update.
             */
            @Override
            public void updateElement(final int index, @NotNull final Pane rowPane)
            {
                final ItemStack resource = tempRes.get(index).getA().getItemStack().copy();
                resource.setCount(resource.getMaxStackSize());

                final Text resourceLabel = rowPane.findPaneOfTypeByID(RESOURCE_NAME, Text.class);
                resourceLabel.setText(resource.getDisplayName());

                final Text quantityLabel = rowPane.findPaneOfTypeByID(QUANTITY_LABEL, Text.class);
                quantityLabel.setText(String.valueOf(tempRes.get(index).getB()));

                rowPane.findPaneOfTypeByID(RESOURCE_ICON, ItemIcon.class).setItem(resource);
            }
        });
    }
}

