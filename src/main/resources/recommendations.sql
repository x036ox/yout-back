use yout;

select v.*, ul.repeats, uc.repeats from video_entity v 
            join video_metadata vm on v.id = vm.id
            join video_like l on l.video_id = v.id
            join user_category uc on uc.metadata_id = 20 and vm.category = uc.category 
            join user_language ul on ul.metadata_id = 20 and vm.language = ul.language 
            where l.timestamp > date_sub(now(), interval 90 day)
            group by v.id 
            order by ul.repeats desc, uc.repeats desc, COUNT(*) DESC;
            
            #order by language repeats, category repeats, amount of likes

