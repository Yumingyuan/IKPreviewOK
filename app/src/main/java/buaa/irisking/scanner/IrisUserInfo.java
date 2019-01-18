package buaa.irisking.scanner;

/**
 * 虹膜信息结构体
 * 
 */
public class IrisUserInfo {
    public int m_Id;
    public String m_Uid;
    public int m_UserFavicon;
    public String m_UserName;
//    public int m_LeftEye;
//    public int m_RightEye;
    public byte[] m_LeftTemplate;
    public byte[] m_RightTemplate;
    public int m_LeftTemplate_Count;
    public int m_RightTemplate_Count;
    public String m_EnrollTime;
}
