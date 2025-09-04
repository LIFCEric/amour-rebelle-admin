package fr.amour.rebelle.admin.dao;

@FunctionalInterface
public interface SQLFunction<C, R> {
    R apply(C c) throws Exception;
}
