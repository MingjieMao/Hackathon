package dao;


import dao.model.Report;


public class ReportDao extends DAO<Report>{


    protected ReportDao() {
        super((r1, r2) -> {
            int c = r1.message.compareTo(r2.message);
            return c != 0 ? c : r1.user.compareTo(r2.user);
        });
    }


    private static ReportDao instance;


    public static ReportDao getInstance() {
        if (instance == null) instance = new ReportDao();
        return instance;
    }
}
