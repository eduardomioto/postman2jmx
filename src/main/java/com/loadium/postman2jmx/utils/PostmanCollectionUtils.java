package com.loadium.postman2jmx.utils;

import java.util.ArrayList;
import java.util.List;

import com.loadium.postman2jmx.model.postman.PostmanCollection;
import com.loadium.postman2jmx.model.postman.PostmanItem;

public class PostmanCollectionUtils {

	private static void getItem(PostmanItem item, List<PostmanItem> itemList, String folderName) {

		if (item.getItems().size() == 0 && item.getRequest() != null) {
			final String index = getIndex(itemList);
			item.setName(index + "_" +  folderName + "_" + item.getName());
			itemList.add(item);
		} else {
			folderName = item.getName();
		}

		for (final PostmanItem i : item.getItems()) {
			getItem(i, itemList, folderName);
		}
	}

	private static String getIndex(List<PostmanItem> itemList) {
		return String.format("%03d", itemList.size() + 1);
	}

	public static List<PostmanItem> getItems(PostmanCollection postmanCollection) {
		final List<PostmanItem> items = new ArrayList<>();

		for (final PostmanItem item : postmanCollection.getItems()) {
			getItem(item, items, null);
		}
		return items;
	}
}
