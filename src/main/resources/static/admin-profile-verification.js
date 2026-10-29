async function loadStudentProfiles(){
  const host=document.querySelector('#students');
  try{
    const profiles=await call('/api/admin/students');
    host.innerHTML=profiles.map(p=>'<article class="card"><h3>'+esc(p.full_name)+' · '+esc(p.email)+'</h3><p>'+esc(p.course_type)+' '+esc(p.course_name)+' year '+esc(p.current_year)+' · '+esc(p.institution_name)+'</p><p>Institution: '+esc(p.institution_approval_status||'Not reviewed')+' ('+(p.institution_approval_verified?'verified':'unverified')+') · Admission route: '+esc(p.admission_route||'missing')+' ('+(p.admission_route_verified?'verified':'unverified')+') · CSSS board: '+esc(p.qualifying_board||'missing')+' / merit '+esc(p.qualifying_merit_position||'missing')+' ('+(p.qualifying_conditions_verified?'verified':'unverified')+')</p><button onclick="verifyStudentProfile('+p.user_id+')">Review profile claims</button></article>').join('')||'No student profiles yet.';
  }catch(e){host.textContent=e.message;}
}
async function verifyStudentProfile(id){
  const approval=prompt('Institution status (APPROVED, PENDING, NOT_APPROVED, NOT_APPLICABLE):');
  if(approval===null)return;
  const remarks=prompt('What did you check? Add your review remarks:');
  if(!remarks)return;
  const routeVerified=confirm('Mark the recorded admission route as reviewed?');
  const csssVerified=confirm('Mark the qualifying board merit details as reviewed?');
  try{
    await call('/api/admin/students/'+id+'/verification',{method:'PUT',body:JSON.stringify({institutionApprovalStatus:approval||null,admissionRouteVerified:routeVerified,qualifyingConditionsVerified:csssVerified,remarks})});
    await loadStudentProfiles();
  }catch(e){alert(e.message);}
}
document.querySelector('#refresh').addEventListener('click',loadStudentProfiles);
setTimeout(loadStudentProfiles,500);
