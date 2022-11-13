package facades;

import dtos.ConventusResourceDTO;
import utils.ConventusResourcesFetcher;
import utils.ConventusResourcesParallelFetcher;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ConventusFacade {
    private static facades.ConventusFacade instance;
    private static EntityManagerFactory emf;

    //Private Constructor to ensure Singleton
    private ConventusFacade() {
    }


    /**
     * @param _emf
     * @return an instance of this facade class.
     */
    public static ConventusFacade getConventusFacade(EntityManagerFactory _emf) {
        if (instance == null) {
            emf = _emf;
            instance = new ConventusFacade();
        }
        return instance;
    }

    private EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public List<ConventusResourceDTO> getBFFInfo(String query) throws IOException {
        ConventusResourcesFetcher conventusResourcesFetcher = new ConventusResourcesFetcher();
        return conventusResourcesFetcher.getBFFInfo(query);
    }

    public List<ConventusResourceDTO> getBFFInfoParallel(String query) throws IOException, ExecutionException, InterruptedException {
        ConventusResourcesParallelFetcher conventusResourcesFetcherParallel = new ConventusResourcesParallelFetcher();
        return conventusResourcesFetcherParallel.getBFFInfo(query);
    }

}


