package util.file;

public class FileUtil {
	
	//static������ String path�� �Ѱ� �޾Ƽ� ��ȯ���� String�� getExt �Լ�
	public static String getExt(String path){/*�Ѱܹ޴� ��ο��� Ȯ���� ���ϱ�*/
		int last =path.lastIndexOf(".");//���� �߰� �߰��� �ִ� .�� �����ϱ� ���ؼ� lastIndexOf�� ����Ͽ��� ������ ������
												   //Ȯ���� ���� �̾Ƴ���
		return path.substring(last+1,path.length());//length�� ���̰� �ƴ϶� ������ ������ ����?���� ����Ų��
		
	}
}
