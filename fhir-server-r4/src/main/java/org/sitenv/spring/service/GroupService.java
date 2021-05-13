package org.sitenv.spring.service;

import java.util.List;
import org.sitenv.spring.model.DafGroup;
import org.sitenv.spring.util.SearchParameterMap;

public interface GroupService {
	DafGroup getGroupById(String paramString);
  List<DafGroup> search(SearchParameterMap paramSearchParameterMap);
  void createGroups(DafGroup paramDafGroup);
}
