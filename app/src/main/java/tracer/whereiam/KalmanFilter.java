package tracer.whereiam;

public class KalmanFilter
{
    public double z_Din=10.0;
    public double Q = 0.0001;
    public double R = 0.0025;
    public double A = 1.0;
    public double B_uk = 0.0;
    public double H = 1.0;
    public double x_Predict = 0.0;
    public double Xk = 25.0;
    public double p_Predict = 0.0;
    public double Pk = 1.0;
    public double K_gain = 1.0;

    public KalmanFilter() {
    }
}