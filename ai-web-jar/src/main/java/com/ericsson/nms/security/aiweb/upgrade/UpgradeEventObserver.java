package com.ericsson.nms.security.aiweb.upgrade;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;

import com.ericsson.oss.itpf.sdk.upgrade.UpgradeEvent;
import com.ericsson.oss.itpf.sdk.upgrade.UpgradePhase;

@ApplicationScoped
public class UpgradeEventObserver {

    //    @Inject
    //    private SystemRecorder systemRecorder;

    @Inject
    private Logger logger;

    public void upgradeNotificationObserver(@Observes final UpgradeEvent event) {

        final UpgradePhase phase = event.getPhase();
        switch (phase) {
        case SERVICE_INSTANCE_UPGRADE_PREPARE:
            logger.info("AI-Web Upgrade Prepare Stage");
            event.accept("OK");
            //  recordEvent("AI-Web has accepted upgrade event", phase);
            break;
        case SERVICE_CLUSTER_UPGRADE_PREPARE:
        case SERVICE_CLUSTER_UPGRADE_FAILED:
        case SERVICE_CLUSTER_UPGRADE_FINISHED_SUCCESSFULLY:
        case SERVICE_INSTANCE_UPGRADE_FAILED:
        case SERVICE_INSTANCE_UPGRADE_FINISHED_SUCCESSFULLY:
            logger.info("AI-Web Upgrade Finished Successfully");
            event.accept("OK");
            // recordEvent("AI-Web has accepted upgrade event", phase);
            break;

        default:
            logger.info("AI-Web has rejected event", phase);
            event.reject("Unexpected UpgradePhase");
            //   recordEvent("AI-Web has rejected event", phase);
            break;

        }

    }
    /**
     * Records Event
     * 
     * @param eventDesc
     *            The event to record
     */
    //    private void recordEvent(final String eventDesc, final UpgradePhase phase) {
    //        systemRecorder.recordEvent(eventDesc + " : " + phase.toString(),
    //                EventLevel.COARSE,
    //                "Upgrade Event : " + phase.toString(),
    //                "AI-Web", "");
    //    }

    /**
     * For Unit Test purposes only
     * 
     * @param systemRecorder
     */
    //    protected void setSystemRecorder(final SystemRecorder systemRecorder) {
    //        this.systemRecorder = systemRecorder;
    //    }
}
