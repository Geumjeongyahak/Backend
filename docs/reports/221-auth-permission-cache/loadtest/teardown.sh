#!/usr/bin/env bash
# provision.sh 가 만든 VM 을 지우고, 디스크·스냅샷이 남지 않았는지 확인한다.
# 라벨 purpose=loadtest-221 이 붙은 것과 이름이 lt221- 로 시작하는 것만 대상이다.
set -euo pipefail
ZONE="${ZONE:-us-east1-c}"
mapfile -t VMS < <(gcloud compute instances list --filter="labels.purpose=loadtest-221" --format='value(name)')
if [[ ${#VMS[@]} -gt 0 ]]; then
  gcloud compute instances delete "${VMS[@]}" --zone "$ZONE" --delete-disks=all --quiet
fi

left_disks=$(gcloud compute disks list --filter="name~^lt221- OR labels.purpose=loadtest-221" --format='value(name)')
left_snaps=$(gcloud compute snapshots list --filter="name~^lt221- OR sourceDisk~/lt221-" --format='value(name)')
left_vms=$(gcloud compute instances list --filter="name~^lt221-" --format='value(name)')
echo "남은 VM: ${left_vms:-없음} / 디스크: ${left_disks:-없음} / 스냅샷: ${left_snaps:-없음}"
[[ -z "$left_vms$left_disks$left_snaps" ]] || exit 1
