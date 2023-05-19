package org.tarasca.mythicalbeings.rgame.omno.service;

import org.tarasca.mythicalbeings.rgame.omno.service.object.Soldier;

import java.util.List;

public class RgameUtility {

    public static void sortListSoldier(List<Soldier> soldierList) {

        if (soldierList == null || soldierList.size() < 2) {
            return;
        }

        boolean modified = true;

        while (modified) {

            modified = false;

            for (int i = 0; i < soldierList.size() - 1; i++) {
                Soldier soldier = soldierList.get(i);
                Soldier soldierNext = soldierList.get(i + 1);

                if (soldier.power < soldierNext.power) {
                    soldierList.set(i, soldierNext);
                    soldierList.set(i + 1, soldier);
                    modified = true;
                } else if (soldier.power == soldierNext.power && soldier.asset > soldierNext.asset) {
                    soldierList.set(i, soldierNext);
                    soldierList.set(i + 1, soldier);
                    modified = true;
                }
            }
        }
    }
}
