/*
 * JTable이 수시로 정보를 얻어가는 컨트롤러
 * */
package oracle;

import java.util.Vector;

import javax.swing.table.AbstractTableModel;

public class MyModel extends AbstractTableModel{
	Vector columnName; //컬럼의 제목을 담을 백터
	Vector<Vector> list; //레코드를 담을 이차원 백터

	public MyModel(Vector list, Vector columnName){
		this.list=list;
		this.columnName=columnName;
	}
	
	public int getColumnCount() {
		return columnName.size();
	}
	
@Override
	public String getColumnName(int col) {

		return (String) columnName.elementAt(col);
	}

	public int getRowCount() {
		return list.size();
	}

	// JTable은 모델에 따라서 편집기능도 좌우된다
	// row,col에 위치한 셀을 편집가능하게 한다
	public boolean isCellEditable(int row, int col) {
		return true;
	}
	//각 셀의 변경값을 반영하는 메서드 오버라이드

	public void setValueAt(Object value, int row, int col) {
		//층, 호수를 변경한다!!
		Vector vec=list.get(row);
		vec.set(col,value);
		
		//table모델의 값이 바뀌는 것을 알리는 메서드
		this.fireTableDataChanged();
		//this.fireTableCellUpdated(row, col);	
	}
	
	
	public Object getValueAt(int row, int col) {	
		Vector vec=list.get(row);
		return vec.elementAt(col);
	}

}
