package nl.ru.crpx.dataobject;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * A list of DataObjects.
 */
public class DataObjectList extends DataObject {

	List<DataObject> list = new ArrayList<DataObject>();

	String xmlElementName;

	public String getElementName() {
		return xmlElementName;
	}

	public DataObjectList(String xmlElementName, DataObject... items) {
		this.xmlElementName = xmlElementName;
		list.addAll(Arrays.asList(items));
	}

	@Override
	public void serialize(Writer out, DataFormat fmt, boolean prettyPrint, int depth) throws IOException {
		switch (fmt) {
		case JSON:
			out.append("[");
			break;
		case XML:
			break;
		}
		boolean first = true;
		depth++;
		for (DataObject value: list) {
			switch (fmt) {
			case JSON:
				if (!first)
					out.append(",");
				if (prettyPrint) {
					out.append("\n");
					indent(out, depth);
				}
				value.serialize(out, fmt, prettyPrint, depth);
				break;
			case XML:
				if (prettyPrint)
					indent(out, depth);
				out.append("<").append(xmlElementName).append(">");
				if (prettyPrint && !value.isSimple()) {
					out.append("\n");
				}
				value.serialize(out, fmt, prettyPrint, depth);
				out.append("</").append(xmlElementName).append(">");
				if (prettyPrint)
					out.append("\n");
				break;
			}
			first = false;
		}
		depth--;
		switch (fmt) {
		case JSON:
			if (prettyPrint) {
				out.append("\n");
				indent(out, depth);
			}
			out.append("]");
			break;
		case XML:
			break;
		}
	}
  
  /**
   * sort
   *    This assumes that the DataObjectList contains
   *    [DataObjectMapElement] members, and nothing else
   * @param sKey 
   */
  public void sort(final String sKey) {
    List<DataObject> lSorted = new ArrayList<>();
    lSorted.addAll(list);

    /*
    Logger.getLogger("sort").debug("Unsorted:");
    for (int i=0;i<lSorted.size();i++) {
      DataObjectMapElement oThis = (DataObjectMapElement) lSorted.get(i);
      Logger.getLogger("sort").debug(i+": "+oThis.get(sKey).toString());
    } */

    // Sor the arraylist
     Comparator c = new Comparator<DataObjectMapElement>() {

      @Override
      public int compare(DataObjectMapElement t, DataObjectMapElement t1) { 
        String s1 = t.get(sKey).toString();
        String s2 = t1.get(sKey).toString();
        int iCmp = s1.compareToIgnoreCase(s2);
        // Logger.getLogger("sort").debug(s1 + "-" + s2 + " = " + iCmp);
        return iCmp;
      }
    };
    // Sort it
    Collections.sort(lSorted, c);
    /*
    Logger.getLogger("sort").debug("Sorted:");
    for (int i=0;i<lSorted.size();i++) {
      DataObjectMapElement oThis = (DataObjectMapElement) lSorted.get(i);
      Logger.getLogger("sort").debug(i+": "+oThis.get(sKey).toString());
    } */
    // Clear the original list
    list.clear();
    // Convert ArrayList to DataObjectList
    list.addAll(lSorted);  
  }

	public int size() {
		return list.size();
	}

	public boolean isEmpty() {
		return list.isEmpty();
	}

	public boolean contains(Object o) {
		return list.contains(o);
	}

	public Iterator<DataObject> iterator() {
		return list.iterator();
	}

	public Object[] toArray() {
		return list.toArray();
	}

	public <T> T[] toArray(T[] a) {
		return list.toArray(a);
	}

	public boolean add(DataObject e) {
		return list.add(e);
	}

	public boolean add(String value) {
		return list.add(DataObject.from(value));
	}

	public boolean add(int value) {
		return list.add(DataObject.from(value));
	}

	public boolean add(long value) {
		return list.add(DataObject.from(value));
	}

	public boolean add(double value) {
		return list.add(DataObject.from(value));
	}

	public boolean add(boolean value) {
		return list.add(DataObject.from(value));
	}

	public boolean remove(Object o) {
		return list.remove(o);
	}

	public boolean containsAll(Collection<?> c) {
		return list.containsAll(c);
	}

	public boolean addAll(Collection<? extends DataObject> c) {
		return list.addAll(c);
	}

	public boolean addAll(int index, Collection<? extends DataObject> c) {
		return list.addAll(index, c);
	}

	public boolean removeAll(Collection<?> c) {
		return list.removeAll(c);
	}

	public boolean retainAll(Collection<?> c) {
		return list.retainAll(c);
	}

	public void clear() {
		list.clear();
	}

	@Override
	public boolean equals(Object o) {
		return list.equals(o);
	}

	@Override
	public int hashCode() {
		return list.hashCode();
	}

	public DataObject get(int index) {
		return list.get(index);
	}

	public DataObject set(int index, DataObject element) {
		return list.set(index, element);
	}

	public void add(int index, DataObject element) {
		list.add(index, element);
	}

	public DataObject remove(int index) {
		return list.remove(index);
	}

	public int indexOf(Object o) {
		return list.indexOf(o);
	}

	public int lastIndexOf(Object o) {
		return list.lastIndexOf(o);
	}

	public ListIterator<DataObject> listIterator() {
		return list.listIterator();
	}

	public ListIterator<DataObject> listIterator(int index) {
		return list.listIterator(index);
	}

	public List<DataObject> subList(int fromIndex, int toIndex) {
		return list.subList(fromIndex, toIndex);
	}

	@Override
	public boolean isSimple() {
		return false;
	}

	/**
	 * Remove map keys with empty values anywhere inside this object.
	 */
	@Override
	public void removeEmptyMapValues() {
		for (DataObject value: list) {
			value.removeEmptyMapValues();
		}
	}


}
