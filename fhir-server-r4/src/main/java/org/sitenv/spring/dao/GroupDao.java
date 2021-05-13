package org.sitenv.spring.dao;

import java.util.List;
import org.sitenv.spring.model.DafGroup;
import org.sitenv.spring.util.SearchParameterMap;

public interface GroupDao {
	DafGroup getGroupById(String paramString);
	void createGroup(DafGroup paramDafGroup);
  List<DafGroup> search(SearchParameterMap paramSearchParameterMap);
}
