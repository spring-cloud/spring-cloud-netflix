package org.springframework.cloud.netflix.turbine;

import java.util.Objects;

public class ClusterInformation {

    private final String name;
    private final String link;

    public ClusterInformation(String name, String link) {
        this.name = name;
        this.link = link;
    }

    public String getName() {
        return name;
    }

    public String getLink() {
        return link;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClusterInformation that = (ClusterInformation) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(link, that.link);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, link);
    }

    @Override
    public String toString() {
        return "ClusterInformation{" +
                "name='" + name + '\'' +
                ", link='" + link + '\'' +
                '}';
    }
}
