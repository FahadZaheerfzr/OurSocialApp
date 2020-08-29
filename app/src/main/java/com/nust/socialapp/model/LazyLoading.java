

package com.nust.socialapp.model;


import com.nust.socialapp.enums.ItemType;

public interface LazyLoading {
    ItemType getItemType();
    void setItemType(ItemType itemType);
}
