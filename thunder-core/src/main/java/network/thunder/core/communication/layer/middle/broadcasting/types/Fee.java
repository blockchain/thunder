package network.thunder.core.communication.layer.middle.broadcasting.types;

public class Fee {
    public int fix;
    public int perc;

    public Fee () {
    }

    public Fee (int fix, int perc) {
        this.fix = fix;
        this.perc = perc;
    }

    public int calculateFee (long paymentAmount) {
        return (int) (fix + (perc * paymentAmount / 1000000));
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Fee fee = (Fee) o;

        if (fix != fee.fix) {
            return false;
        }
        return perc == fee.perc;

    }

    @Override
    public int hashCode () {
        int result = fix;
        result = 31 * result + perc;
        return result;
    }

    @Override
    public String toString () {
        return "Fee{" +
                "fix=" + fix +
                ", perc=" + perc +
                '}';
    }
}
