package util.file;

public class FileUtil {
	
	//static선언의 String path를 넘겨 받아서 반환형이 String인 getExt 함수
	public static String getExt(String path){/*넘겨받는 경로에서 확장자 구하기*/
		int last =path.lastIndexOf(".");//파일 중간 중간에 있는 .을 방지하기 위해서 lastIndexOf를 사용하여서 마지막 파일의
												   //확장자 명을 뽑아낸다
		return path.substring(last+1,path.length());//length는 길이가 아니라 마지막 글자의 번지?수를 가리킨다
		
	}
}
