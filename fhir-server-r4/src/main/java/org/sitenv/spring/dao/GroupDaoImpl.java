package org.sitenv.spring.dao;

import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import java.util.List;
import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;
import org.sitenv.spring.model.DafGroup;
import org.sitenv.spring.util.SearchParameterMap;
import org.springframework.stereotype.Repository;

@Repository("GroupDao")
public class GroupDaoImpl extends AbstractDao implements GroupDao {
	
	public DafGroup getGroupById(String id) {
	    Criteria criteria = getSession().createCriteria(DafGroup.class).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
	    criteria.add(Restrictions.sqlRestriction("{alias}.data->>'id' = '" + id + "' order by {alias}.data->'meta'->>'versionId' desc"));
	    return (DafGroup) criteria.list().get(0);
	  }
	
	public void createGroup(DafGroup dafGroup) {
	    getSession().saveOrUpdate(dafGroup);
	  }
  public List<DafGroup> search(SearchParameterMap theMap) {
    Criteria criteria = getSession().createCriteria(DafGroup.class).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
    buildIdCriteria(theMap, criteria);
    buildIdentifierCriteria(theMap, criteria);
    buildNameCriteria(theMap, criteria);
    return criteria.list();
  }
  
  private void buildIdCriteria(SearchParameterMap theMap, Criteria criteria) {
    List<List<? extends IQueryParameterType>> list = (List<List<? extends IQueryParameterType>>)theMap.get("_id");
    if (list != null)
      for (List<? extends IQueryParameterType> values : list) {
        for (IQueryParameterType params : values) {
          StringParam id = (StringParam)params;
          if (id.getValue() != null)
            criteria.add(Restrictions.sqlRestriction("{alias}.data->>'id' = '" + id.getValue() + "'")); 
        } 
      }  
  }
  
  private void buildIdentifierCriteria(SearchParameterMap theMap, Criteria criteria) {
    List<List<? extends IQueryParameterType>> list = (List<List<? extends IQueryParameterType>>)theMap.get("identifier");
    if (list != null)
      for (List<? extends IQueryParameterType> values : list) {
        Disjunction disjunction = Restrictions.disjunction();
        for (IQueryParameterType params : values) {
          TokenParam identifier = (TokenParam)params;
          Criterion orCond = null;
          if (identifier.getValue() != null)
        	  orCond = Restrictions.or(new Criterion[] { Restrictions.sqlRestriction("{alias}.data->'identifier'->0->>'value' ilike '%" + identifier
                    .getValue() + "%'"), 
                  Restrictions.sqlRestriction("{alias}.data->'identifier'->1->>'value' ilike '%" + identifier
                    .getValue() + "%'"), 
                  
                  Restrictions.sqlRestriction("{alias}.data->'identifier'->0->>'system' ilike '%" + identifier
                    .getValue() + "%'"), 
                  Restrictions.sqlRestriction("{alias}.data->'identifier'->1->>'system' ilike '%" + identifier
                    .getValue() + "%'") }); 
          disjunction.add(orCond);
        } 
        criteria.add((Criterion)disjunction);
      }  
  }
  
  private void buildNameCriteria(SearchParameterMap theMap, Criteria criteria) {
    List<List<? extends IQueryParameterType>> list = (List<List<? extends IQueryParameterType>>)theMap.get("name");
    if (list != null)
      for (List<? extends IQueryParameterType> values : list) {
        for (IQueryParameterType params : values) {
          StringParam name = (StringParam)params;
          if (name.getValue() != null)
            criteria.add(Restrictions.sqlRestriction("{alias}.data->>'name' = '" + name.getValue() + "'")); 
        } 
      }  
  }
}
